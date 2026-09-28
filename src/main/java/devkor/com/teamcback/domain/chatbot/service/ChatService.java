package devkor.com.teamcback.domain.chatbot.service;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import devkor.com.teamcback.domain.chatbot.dto.ClientAction;
import devkor.com.teamcback.domain.chatbot.dto.ClientActionType;
import devkor.com.teamcback.domain.chatbot.dto.NavigateRouteAction;
import devkor.com.teamcback.domain.chatbot.dto.PendingLocationRef;
import devkor.com.teamcback.domain.chatbot.dto.PendingRouteState;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.request.CurrentLocationReq;
import devkor.com.teamcback.domain.chatbot.dto.response.ChatMessageRes;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatService {
    static final String SYSTEM_PROMPT = """
            You are a concise Korean Korea University campus assistant.
            Answer general conversation naturally and use concise, practical Korean.
            Dynamic campus facts may be answered only when reliable campus context is already provided.
            If current or changing campus information is not present in context, do not guess; explain that
            the information is unavailable or ask the user to clarify.
            Never expose private data, authentication data, raw coordinates, or internal identifiers.
            """;

    private final ChatOrchestrator chatOrchestrator;
    private final ChatSessionMemoryService memoryService;
    private final ChatRateLimiter rateLimiter;
    private final PendingRouteStateService pendingRouteStateService;
    private final CrowdStatusWorkflow crowdStatusWorkflow;
    private final ChatRequestRouter requestRouter;
    private final LocationDetailWorkflow locationDetailWorkflow;
    private final PlaceReviewsWorkflow placeReviewsWorkflow;
    private final CampusStatusWorkflow campusStatusWorkflow;
    private final MenuWorkflow menuWorkflow;
    private final FacilityWorkflow facilityWorkflow;
    private final RoomCourseWorkflow roomCourseWorkflow;

    /** Compatibility constructor for focused unit tests that do not exercise pending state. */
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter) {
        this(chatOrchestrator, memoryService, rateLimiter, null, null, new ChatRequestRouter(), null, null, null, null, null, null);
    }

    /** Compatibility constructor for tests that provide pending-route state only. */
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter, PendingRouteStateService pendingRouteStateService) {
        this(chatOrchestrator, memoryService, rateLimiter, pendingRouteStateService, null, new ChatRequestRouter(), null, null, null, null, null, null);
    }

    /** Compatibility constructor for focused tests that provide the existing Crowd workflow. */
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter, PendingRouteStateService pendingRouteStateService,
                       CrowdStatusWorkflow crowdStatusWorkflow) {
        this(chatOrchestrator, memoryService, rateLimiter, pendingRouteStateService,
                crowdStatusWorkflow, new ChatRequestRouter(), null, null, null, null, null, null);
    }

    @Autowired
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter,
                       PendingRouteStateService pendingRouteStateService,
                       CrowdStatusWorkflow crowdStatusWorkflow,
                       ChatRequestRouter requestRouter,
                       LocationDetailWorkflow locationDetailWorkflow,
                       PlaceReviewsWorkflow placeReviewsWorkflow,
                       CampusStatusWorkflow campusStatusWorkflow,
                       MenuWorkflow menuWorkflow, FacilityWorkflow facilityWorkflow,
                       RoomCourseWorkflow roomCourseWorkflow) {
        this.chatOrchestrator = chatOrchestrator;
        this.memoryService = memoryService;
        this.rateLimiter = rateLimiter;
        this.pendingRouteStateService = pendingRouteStateService;
        this.crowdStatusWorkflow = crowdStatusWorkflow;
        this.requestRouter = requestRouter;
        this.locationDetailWorkflow = locationDetailWorkflow;
        this.placeReviewsWorkflow = placeReviewsWorkflow;
        this.campusStatusWorkflow = campusStatusWorkflow;
        this.menuWorkflow = menuWorkflow;
        this.facilityWorkflow = facilityWorkflow;
        this.roomCourseWorkflow = roomCourseWorkflow;
    }

    /** Compatibility constructor retained for existing workflow-focused tests. */
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter, PendingRouteStateService pendingRouteStateService,
                       CrowdStatusWorkflow crowdStatusWorkflow, ChatRequestRouter requestRouter,
                       LocationDetailWorkflow locationDetailWorkflow,
                       PlaceReviewsWorkflow placeReviewsWorkflow,
                       CampusStatusWorkflow campusStatusWorkflow) {
        this(chatOrchestrator, memoryService, rateLimiter, pendingRouteStateService, crowdStatusWorkflow,
                requestRouter, locationDetailWorkflow, placeReviewsWorkflow, campusStatusWorkflow,
                null, null, null);
    }

    /** Compatibility constructor for callers that only wire the PR1 routing boundary. */
    public ChatService(ChatOrchestrator chatOrchestrator, ChatSessionMemoryService memoryService,
                       ChatRateLimiter rateLimiter, PendingRouteStateService pendingRouteStateService,
                       CrowdStatusWorkflow crowdStatusWorkflow, ChatRequestRouter requestRouter) {
        this(chatOrchestrator, memoryService, rateLimiter, pendingRouteStateService, crowdStatusWorkflow,
                requestRouter, null, null, null, null, null, null);
    }

    public ChatMessageRes sendMessage(ChatMessageReq request, ChatCaller caller) {
        UUID sessionId = request.sessionId() == null ? UUID.randomUUID() : request.sessionId();
        rateLimiter.check(caller);
        var history = memoryService.load(sessionId, caller);
        PendingRouteState pending = loadPending(sessionId, caller);
        ChatRequestRouter.PendingWorkflow pendingWorkflow = activePendingWorkflow(sessionId, caller, pending);
        ChatRequestRouter.RoutingDecision routing = requestRouter.route(pendingWorkflow, request.message());
        if (routing.route() != ChatRequestRouter.Route.CONTINUE_PENDING) {
            releaseSupersededWorkflows(sessionId, caller, pending);
        }
        if (crowdStatusWorkflow != null
                && routing.workflowType() == ChatRequestRouter.WorkflowType.CROWD
                && (routing.route() == ChatRequestRouter.Route.CONTINUE_PENDING
                || routing.route() == ChatRequestRouter.Route.NEW_INTENT)) {
            CrowdStatusWorkflow.CrowdWorkflowResult crowd = crowdStatusWorkflow.handle(
                    sessionId, caller, toGatewayHistory(history), request.message());
            if (crowd.handled()) {
                String reply = crowd.reply() == null || crowd.reply().isBlank()
                        ? "혼잡도 요청을 처리하지 못했어요." : crowd.reply();
                memoryService.save(sessionId, caller, request.message(), reply);
                return new ChatMessageRes(sessionId, reply, null);
            }
        }
        if (locationDetailWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.LOCATION_DETAIL
                && (routing.route() == ChatRequestRouter.Route.CONTINUE_PENDING
                || routing.route() == ChatRequestRouter.Route.NEW_INTENT)) {
            LocationDetailWorkflow.WorkflowResult location = locationDetailWorkflow.handle(
                    sessionId, caller, toGatewayHistory(history), request.message());
            if (location.handled()) return saveWorkflowReply(sessionId, caller, request.message(), location.reply());
        }
        if (placeReviewsWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.REVIEW
                && (routing.route() == ChatRequestRouter.Route.CONTINUE_PENDING
                || routing.route() == ChatRequestRouter.Route.NEW_INTENT)) {
            PlaceReviewsWorkflow.WorkflowResult review = placeReviewsWorkflow.handle(
                    sessionId, caller, toGatewayHistory(history), request.message());
            if (review.handled()) return saveWorkflowReply(sessionId, caller, request.message(), review.reply());
        }
        if (campusStatusWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.CAMPUS_STATUS
                && routing.route() == ChatRequestRouter.Route.NEW_INTENT) {
            CampusStatusWorkflow.WorkflowResult status = campusStatusWorkflow.handle(sessionId, caller, request.message());
            return saveWorkflowReply(sessionId, caller, request.message(), status.reply());
        }
        if (menuWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.MENU) {
            MenuWorkflow.WorkflowResult menu = menuWorkflow.handle(sessionId, caller, toGatewayHistory(history), request.message());
            if (menu.handled()) return saveWorkflowReply(sessionId, caller, request.message(), menu.reply());
        }
        if (facilityWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.FACILITY) {
            FacilityWorkflow.WorkflowResult facility = facilityWorkflow.handle(sessionId, caller, toGatewayHistory(history), request.message());
            if (facility.handled()) return saveWorkflowReply(sessionId, caller, request.message(), facility.reply());
        }
        if (roomCourseWorkflow != null && routing.workflowType() == ChatRequestRouter.WorkflowType.ROOM_COURSE) {
            RoomCourseWorkflow.WorkflowResult room = roomCourseWorkflow.handle(sessionId, caller, toGatewayHistory(history), request.message());
            if (room.handled()) return saveWorkflowReply(sessionId, caller, request.message(), room.reply());
        }
        if (routing.route() == ChatRequestRouter.Route.CONTINUE_PENDING
                && routing.workflowType() == ChatRequestRouter.WorkflowType.ROUTE) {
            pending = loadPending(sessionId, caller);
        } else {
            pending = null;
        }
        boolean generalChat = routing.workflowType() == ChatRequestRouter.WorkflowType.GENERAL_CHAT;
        ChatOrchestrationResult result = generalChat
                ? chatOrchestrator.executeGeneral(SYSTEM_PROMPT, toGatewayHistory(history), request.message())
                : chatOrchestrator.execute(promptWithPendingState(pending),
                toGatewayHistory(history), messageWithRequestContext(request));
        ClientAction action = assembleRouteAction(result.resolvedLocations());
        if (action != null) {
            deletePending(sessionId, caller);
        } else {
            savePendingIfRouteIsIncomplete(sessionId, caller, pending, result.searchResolutions());
        }
        boolean completionFailed = result.reply() == null || result.reply().isBlank();
        if (completionFailed && action == null) {
            String clarification = deterministicClarification(result.searchResolutions());
            if (clarification != null) {
                memoryService.save(sessionId, caller, request.message(), clarification);
                return new ChatMessageRes(sessionId, clarification, null);
            }
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
        String reply = action == null ? result.reply() : routeActionReply(action);
        memoryService.save(sessionId, caller, request.message(), reply);
        return new ChatMessageRes(sessionId, reply, action);
    }

    private ChatRequestRouter.PendingWorkflow activePendingWorkflow(UUID sessionId, ChatCaller caller,
                                                                     PendingRouteState pendingRoute) {
        if (crowdStatusWorkflow != null && crowdStatusWorkflow.hasPending(sessionId, caller)) {
            return ChatRequestRouter.PendingWorkflow.CROWD;
        }
        if (locationDetailWorkflow != null && locationDetailWorkflow.hasPending(sessionId, caller)) {
            return ChatRequestRouter.PendingWorkflow.LOCATION_DETAIL;
        }
        if (placeReviewsWorkflow != null && placeReviewsWorkflow.hasPending(sessionId, caller)) {
            return ChatRequestRouter.PendingWorkflow.REVIEW;
        }
        if (menuWorkflow != null && menuWorkflow.hasPending(sessionId, caller)) return ChatRequestRouter.PendingWorkflow.MENU;
        if (facilityWorkflow != null && facilityWorkflow.hasPending(sessionId, caller)) return ChatRequestRouter.PendingWorkflow.FACILITY;
        if (roomCourseWorkflow != null && roomCourseWorkflow.hasPending(sessionId, caller)) return ChatRequestRouter.PendingWorkflow.ROOM_COURSE;
        return pendingRoute == null
                ? ChatRequestRouter.PendingWorkflow.NONE : ChatRequestRouter.PendingWorkflow.ROUTE;
    }

    private void releaseSupersededWorkflows(UUID sessionId, ChatCaller caller,
                                            PendingRouteState pendingRoute) {
        if (crowdStatusWorkflow != null && crowdStatusWorkflow.hasPending(sessionId, caller)) {
            crowdStatusWorkflow.cancel(sessionId, caller);
        }
        if (locationDetailWorkflow != null && locationDetailWorkflow.hasPending(sessionId, caller)) {
            locationDetailWorkflow.cancel(sessionId, caller);
        }
        if (placeReviewsWorkflow != null && placeReviewsWorkflow.hasPending(sessionId, caller)) {
            placeReviewsWorkflow.cancel(sessionId, caller);
        }
        if (menuWorkflow != null && menuWorkflow.hasPending(sessionId, caller)) menuWorkflow.cancel(sessionId, caller);
        if (facilityWorkflow != null && facilityWorkflow.hasPending(sessionId, caller)) facilityWorkflow.cancel(sessionId, caller);
        if (roomCourseWorkflow != null && roomCourseWorkflow.hasPending(sessionId, caller)) roomCourseWorkflow.cancel(sessionId, caller);
        if (pendingRoute != null) {
            deletePending(sessionId, caller);
        }
    }

    private PendingRouteState loadPending(UUID sessionId, ChatCaller caller) {
        return pendingRouteStateService == null ? null
                : pendingRouteStateService.load(sessionId, caller).orElse(null);
    }

    private ChatMessageRes saveWorkflowReply(UUID sessionId, ChatCaller caller, String userMessage, String reply) {
        String safeReply = reply == null || reply.isBlank() ? "요청을 처리하지 못했어요." : reply;
        memoryService.save(sessionId, caller, userMessage, safeReply);
        return new ChatMessageRes(sessionId, safeReply, null);
    }

    private void deletePending(UUID sessionId, ChatCaller caller) {
        if (pendingRouteStateService != null) {
            pendingRouteStateService.delete(sessionId, caller);
        }
    }

    private String promptWithPendingState(PendingRouteState pending) {
        if (pending == null) {
            return SYSTEM_PROMPT;
        }
        StringBuilder context = new StringBuilder(SYSTEM_PROMPT);
        context.append("\n\n[PENDING_ROUTE_CONTINUATION]\n")
                .append("interaction=").append(pending.interactionType()).append("\n")
                .append("unresolvedRole=").append(pending.unresolvedRole()).append("\n");
        if (pending.resolvedStart() != null) {
            context.append("resolvedStartName=").append(pending.resolvedStart().name()).append("\n");
        }
        if (pending.resolvedEnd() != null) {
            context.append("resolvedEndName=").append(pending.resolvedEnd().name()).append("\n");
        }
        if (!pending.ambiguousCandidates().isEmpty()) {
            context.append("ambiguousCandidateNames=")
                    .append(pending.ambiguousCandidates().stream().map(PendingLocationRef::name)
                            .collect(Collectors.joining(", "))).append("\n");
        }
        context.append("conditions=").append(pending.conditions()).append("\n");
        context.append("Use ROUTE_BEHAVIOR for continuation: if related, re-search both endpoints in this request "
                + "with explicit roles and the pending route intent; never reuse an old ID. If unrelated, handle normally.\n"
                + "[/PENDING_ROUTE_CONTINUATION]");
        return context.toString();
    }

    private void savePendingIfRouteIsIncomplete(UUID sessionId, ChatCaller caller, PendingRouteState previous,
                                                 List<SearchResolutionTrace> traces) {
        if (pendingRouteStateService == null || traces == null) {
            return;
        }
        List<SearchResolutionTrace> routeTraces = traces.stream()
                .filter(trace -> trace.intent() == SearchCampusIntent.NAVIGATE_ROUTE
                        || trace.intent() == SearchCampusIntent.TEXT_ROUTE).toList();
        if (routeTraces.isEmpty()) {
            return;
        }
        Map<SearchCampusRole, SearchResolutionTrace> latest = routeTraces.stream()
                .collect(Collectors.toMap(SearchResolutionTrace::role, Function.identity(), (first, second) -> second));
        SearchResolutionTrace startTrace = latest.get(SearchCampusRole.START);
        SearchResolutionTrace endTrace = latest.get(SearchCampusRole.END);
        boolean hasAmbiguous = routeTraces.stream().anyMatch(SearchResolutionTrace::ambiguous);
        if (!hasAmbiguous && previous == null) {
            return;
        }
        PendingLocationRef start = uniqueCandidate(startTrace);
        PendingLocationRef end = uniqueCandidate(endTrace);
        if (start == null && previous != null) {
            start = previous.resolvedStart();
        }
        if (end == null && previous != null) {
            end = previous.resolvedEnd();
        }
        SearchCampusRole unresolved = unresolvedRole(startTrace, endTrace, previous);
        if (unresolved == null) {
            return;
        }
        SearchResolutionTrace unresolvedTrace = unresolved == SearchCampusRole.START ? startTrace : endTrace;
        List<PendingLocationRef> candidates = unresolvedTrace == null
                ? (previous == null ? List.of() : previous.ambiguousCandidates())
                : deduplicateCandidates(unresolvedTrace.candidates());
        List<devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition> conditions = routeTraces.stream()
                .flatMap(trace -> trace.conditions().stream()).collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();
        if (conditions.isEmpty() && previous != null) {
            conditions = previous.conditions();
        }
        ResolvedLocation.RouteIntent interaction = routeTraces.stream()
                .anyMatch(trace -> trace.intent() == SearchCampusIntent.TEXT_ROUTE)
                ? ResolvedLocation.RouteIntent.TEXT_ROUTE : ResolvedLocation.RouteIntent.NAVIGATE_ROUTE;
        pendingRouteStateService.save(sessionId, caller,
                new PendingRouteState(interaction, start, end, unresolved, conditions, candidates));
    }

    private String deterministicClarification(List<SearchResolutionTrace> traces) {
        if (traces == null) {
            return null;
        }
        SearchResolutionTrace ambiguous = traces.stream()
                .filter(trace -> trace.intent() == SearchCampusIntent.NAVIGATE_ROUTE
                        || trace.intent() == SearchCampusIntent.TEXT_ROUTE)
                .filter(SearchResolutionTrace::ambiguous)
                .filter(trace -> !trace.candidates().isEmpty())
                .reduce((first, second) -> second)
                .orElse(null);
        if (ambiguous == null) {
            return null;
        }
        List<String> names = deduplicateCandidates(ambiguous.candidates()).stream()
                .map(PendingLocationRef::name)
                .toList();
        if (names.isEmpty()) {
            return null;
        }
        String label = ambiguous.role() == SearchCampusRole.START ? "출발지" : "도착지";
        return label + " 후보가 여러 개 있어요. " + String.join(", ", names) + " 중 어디로 갈까요?";
    }

    private List<PendingLocationRef> deduplicateCandidates(List<PendingLocationRef> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<String, PendingLocationRef> unique = new java.util.LinkedHashMap<>();
        for (PendingLocationRef candidate : candidates) {
            if (candidate == null || candidate.locationType() == null || candidate.locationId() == null) {
                continue;
            }
            unique.putIfAbsent(candidate.locationType() + ":" + candidate.locationId(), candidate);
        }
        return List.copyOf(unique.values());
    }

    private PendingLocationRef uniqueCandidate(SearchResolutionTrace trace) {
        if (trace == null || trace.ambiguous() || trace.candidates().size() != 1) {
            return null;
        }
        return trace.candidates().get(0);
    }

    private SearchCampusRole unresolvedRole(SearchResolutionTrace startTrace, SearchResolutionTrace endTrace,
                                             PendingRouteState previous) {
        if (startTrace != null && (startTrace.ambiguous() || uniqueCandidate(startTrace) == null)) {
            return SearchCampusRole.START;
        }
        if (endTrace != null && (endTrace.ambiguous() || uniqueCandidate(endTrace) == null)) {
            return SearchCampusRole.END;
        }
        return previous == null ? null : previous.unresolvedRole();
    }

    private ClientAction assembleRouteAction(List<ResolvedLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        List<ResolvedLocation> navigate = locations.stream()
                .filter(location -> location.intent() == ResolvedLocation.RouteIntent.NAVIGATE_ROUTE)
                .filter(location -> location.role() != null)
                .filter(location -> location.type() == RouteEndpointType.BUILDING
                        || location.type() == RouteEndpointType.PLACE)
                .toList();
        ResolvedLocation start = uniqueRole(navigate, ResolvedLocation.EndpointRole.START);
        ResolvedLocation end = uniqueRole(navigate, ResolvedLocation.EndpointRole.END);
        if (start == null || end == null || start.id() == null || end.id() == null) {
            return null;
        }
        if (start.type() == end.type() && start.id().equals(end.id())) {
            return null;
        }
        LinkedHashSet<devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition> conditions = new LinkedHashSet<>();
        conditions.addAll(start.conditions());
        conditions.addAll(end.conditions());
        return new ClientAction(ClientActionType.NAVIGATE_ROUTE,
                new NavigateRouteAction(start.type(), start.id(), start.name(), end.type(), end.id(), end.name(),
                        conditions.stream().toList()));
    }

    private ResolvedLocation uniqueRole(List<ResolvedLocation> locations, ResolvedLocation.EndpointRole role) {
        List<ResolvedLocation> matches = locations.stream().filter(location -> location.role() == role).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private String routeActionReply(ClientAction action) {
        NavigateRouteAction route = action.payload();
        return route.startName() + "에서 " + route.endName() + "까지 길찾기 화면으로 안내할게요.";
    }

    private List<LlmGateway.ConversationMessage> toGatewayHistory(
            List<ChatSessionMemoryService.ChatTurn> history) {
        return history.stream()
                .flatMap(turn -> java.util.stream.Stream.of(
                        new LlmGateway.ConversationMessage(LlmGateway.Role.USER, turn.userMessage()),
                        new LlmGateway.ConversationMessage(LlmGateway.Role.ASSISTANT, turn.assistantReply())))
                .toList();
    }

    private String messageWithRequestContext(ChatMessageReq request) {
        String message = request.message();
        if (request.context() == null || request.context().currentLocation() == null) {
            return message;
        }
        CurrentLocationReq location = request.context().currentLocation();
        return message + "\n\n[REQUEST_CONTEXT: currentLocation is available only for this request; "
                + "use start/end type COORD when needed; latitude=" + location.latitude()
                + ", longitude=" + location.longitude() + "; never reveal these raw coordinates]";
    }
}
