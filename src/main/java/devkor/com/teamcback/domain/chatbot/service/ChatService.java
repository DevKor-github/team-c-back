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

            [GROUNDING]
            Use read-only campus tools for dynamic campus facts. Tool results are the only source of truth.
            Never invent locations, IDs, routes, menus, operating hours, crowd data, or reviews. Do not expose
            private data, authentication data, raw coordinates, or internal identifiers. Prefer the latest user
            correction or negation over older conversation and never select a denied candidate again. Use concise,
            practical Korean. If data is unavailable, say so. Do not end with only a future promise such as
            "찾아보겠습니다" when a required Tool can be called. Stay within the six-call limit.

            Future-only responses are prohibited when a Tool can complete the request.
            [ROUTE_BEHAVIOR]
            Decide intent first: NAVIGATE_ROUTE opens the existing route screen; TEXT_ROUTE returns route facts in chat.
            NAVIGATE_ROUTE requires searchCampus(query, role=START, intent=NAVIGATE_ROUTE) and
            searchCampus(query, role=END, intent=NAVIGATE_ROUTE). Preserve specific place wording, use only
            actual Tool results, and do not call findRoute. Once both endpoints are unique, the backend creates
            NAVIGATE_ROUTE ClientAction; never invent or write an Action ID.
            TEXT_ROUTE requires searchCampus for START and END with intent=TEXT_ROUTE, then findRoute only after
            both endpoints are unique. Answer only from findRoute.
            Allowed route values are exactly START/END and NAVIGATE_ROUTE/TEXT_ROUTE. Do not create other role or
            intent values, and do not create NODE/COORD Actions.
            Ambiguous searchCampus results must not be guessed, merged, or invented. Ask using only actual candidate
            names and create no Action until unique.
            If PENDING_ROUTE_CONTINUATION is present and the current message is related, re-search both endpoints in
            this request with explicit roles and NAVIGATE_ROUTE; never copy a previous ID. Unrelated requests stay
            unrelated. Use only supported conditions; BARRIERFREE excludes stair nodes where supported and is not a
            complete accessibility guarantee.
            Examples: UI route -> START search, END search, no findRoute; text route -> START search, END search,
            findRoute; pending END clarification -> re-search both endpoints; ambiguous END -> ask which candidate.
            """;

    private final LlmGateway llmGateway;
    private final ChatSessionMemoryService memoryService;
    private final ChatRateLimiter rateLimiter;
    private final PendingRouteStateService pendingRouteStateService;

    /** Compatibility constructor for focused unit tests that do not exercise pending state. */
    public ChatService(LlmGateway llmGateway, ChatSessionMemoryService memoryService, ChatRateLimiter rateLimiter) {
        this(llmGateway, memoryService, rateLimiter, null);
    }

    @Autowired
    public ChatService(LlmGateway llmGateway, ChatSessionMemoryService memoryService, ChatRateLimiter rateLimiter,
                       PendingRouteStateService pendingRouteStateService) {
        this.llmGateway = llmGateway;
        this.memoryService = memoryService;
        this.rateLimiter = rateLimiter;
        this.pendingRouteStateService = pendingRouteStateService;
    }

    public ChatMessageRes sendMessage(ChatMessageReq request, ChatCaller caller) {
        UUID sessionId = request.sessionId() == null ? UUID.randomUUID() : request.sessionId();
        rateLimiter.check(caller);
        var history = memoryService.load(sessionId, caller);
        PendingRouteState pending = loadPending(sessionId, caller);
        LlmGateway.LlmResult result = llmGateway.generate(promptWithPendingState(pending), toGatewayHistory(history),
                messageWithRequestContext(request));
        ClientAction action = assembleRouteAction(result.resolvedLocations());
        if (action != null) {
            deletePending(sessionId, caller);
        } else {
            savePendingIfRouteIsIncomplete(sessionId, caller, pending, result.searchResolutions());
        }
        boolean completionFailed = result.completionStatus() == LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION
                || result.reply() == null || result.reply().isBlank();
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

    private PendingRouteState loadPending(UUID sessionId, ChatCaller caller) {
        return pendingRouteStateService == null ? null
                : pendingRouteStateService.load(sessionId, caller).orElse(null);
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
                .append("interaction=NAVIGATE_ROUTE\n")
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
        context.append("Use ROUTE_BEHAVIOR for continuation: if related, re-search both endpoints in this request "
                + "with explicit roles and NAVIGATE_ROUTE; never reuse an old ID. If unrelated, handle normally.\n"
                + "[/PENDING_ROUTE_CONTINUATION]");
        return context.toString();
    }

    private void savePendingIfRouteIsIncomplete(UUID sessionId, ChatCaller caller, PendingRouteState previous,
                                                 List<SearchResolutionTrace> traces) {
        if (pendingRouteStateService == null || traces == null) {
            return;
        }
        List<SearchResolutionTrace> routeTraces = traces.stream()
                .filter(trace -> trace.intent() == SearchCampusIntent.NAVIGATE_ROUTE).toList();
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
        pendingRouteStateService.save(sessionId, caller,
                new PendingRouteState(ResolvedLocation.RouteIntent.NAVIGATE_ROUTE, start, end, unresolved,
                        conditions, candidates));
    }

    private String deterministicClarification(List<SearchResolutionTrace> traces) {
        if (traces == null) {
            return null;
        }
        SearchResolutionTrace ambiguous = traces.stream()
                .filter(trace -> trace.intent() == SearchCampusIntent.NAVIGATE_ROUTE)
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
