package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Owns one message's request-local state and deterministic workflow invariants. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatOrchestrator {
    private final LlmGateway llmGateway;
    private final CampusToolAdapter campusToolAdapter;
    private final RoutePlanner routePlanner;

    /** Compatibility constructor for focused tests that exercise the legacy non-route path. */
    public ChatOrchestrator(LlmGateway llmGateway, CampusToolAdapter campusToolAdapter) {
        this(llmGateway, campusToolAdapter, new RoutePlanner(llmGateway));
    }

    @Autowired
    public ChatOrchestrator(LlmGateway llmGateway, CampusToolAdapter campusToolAdapter, RoutePlanner routePlanner) {
        this.llmGateway = llmGateway;
        this.campusToolAdapter = campusToolAdapter;
        this.routePlanner = routePlanner;
    }

    public ChatOrchestrationResult execute(String systemPrompt, List<LlmGateway.ConversationMessage> history,
                                           String userMessage) {
        ResolvedLocationCollector state = new ResolvedLocationCollector();
        RoutePlan routePlan = routePlanner.plan(systemPrompt, history, userMessage);
        if (routePlan.isRoute()) {
            return executeRoutePlan(routePlan, state);
        }
        LlmGateway.LlmResult llmResult = llmGateway.generate(systemPrompt, history, userMessage, state);

        ensureTextRouteExecution(state);
        String reply = llmResult.reply();
        if ((reply == null || reply.isBlank()) && hasSuccessfulTextRoute(state.routeExecution())) {
            reply = deterministicTextRouteReply(state.routeExecution());
        }

        return new ChatOrchestrationResult(reply, llmResult.completionStatus(), state.snapshot(),
                state.searchResolutionSnapshot(), state.routeExecution());
    }

    /** General conversation path: no route planner and no campus capability execution. */
    public ChatOrchestrationResult executeGeneral(String systemPrompt,
                                                  List<LlmGateway.ConversationMessage> history,
                                                  String userMessage) {
        ResolvedLocationCollector state = new ResolvedLocationCollector();
        LlmGateway.LlmResult result = llmGateway.generate(systemPrompt, history, userMessage, state);
        return new ChatOrchestrationResult(result.reply(), result.completionStatus(),
                state.snapshot(), state.searchResolutionSnapshot(), state.routeExecution());
    }

    private ChatOrchestrationResult executeRoutePlan(RoutePlan plan, ResolvedLocationCollector state) {
        SearchCampusIntent intent = plan.intent() == RoutePlan.Intent.TEXT_ROUTE
                ? SearchCampusIntent.TEXT_ROUTE : SearchCampusIntent.NAVIGATE_ROUTE;
        List<RouteCondition> conditions = plan.conditions();
        resolve(state, plan.startQuery(), SearchCampusRole.START, intent, conditions);
        if (!hasUnique(state, SearchCampusRole.START, intent)) {
            return new ChatOrchestrationResult(null, List.of(), LlmGateway.CompletionStatus.COMPLETE,
                    state.searchResolutionSnapshot(), null);
        }
        resolve(state, plan.endQuery(), SearchCampusRole.END, intent, conditions);
        if (!hasUnique(state, SearchCampusRole.END, intent)) {
            return new ChatOrchestrationResult(null, state.snapshot(), LlmGateway.CompletionStatus.COMPLETE,
                    state.searchResolutionSnapshot(), null);
        }

        if (plan.intent() == RoutePlan.Intent.TEXT_ROUTE) {
            FindRouteToolRequest request = state.currentRouteRequest(
                    ResolvedLocation.RouteIntent.TEXT_ROUTE);
            if (request != null) {
                FindRouteToolResult result = campusToolAdapter.findRoute(request);
                state.recordRouteExecution(request, result);
                String reply = hasSuccessfulTextRoute(state.routeExecution())
                        ? deterministicTextRouteReply(state.routeExecution()) : null;
                return new ChatOrchestrationResult(reply, state.snapshot(), LlmGateway.CompletionStatus.COMPLETE,
                        state.searchResolutionSnapshot(), state.routeExecution());
            }
        }
        return new ChatOrchestrationResult(null, state.snapshot(), LlmGateway.CompletionStatus.COMPLETE,
                state.searchResolutionSnapshot(), null);
    }

    private void resolve(ResolvedLocationCollector state, String query, SearchCampusRole role,
                         SearchCampusIntent intent, List<RouteCondition> conditions) {
        SearchCampusToolRequest request = new SearchCampusToolRequest(query, 5, role, intent, conditions);
        SearchCampusToolResult result = campusToolAdapter.searchCampus(request);
        state.record(request, result);
    }

    private boolean hasUnique(ResolvedLocationCollector state, SearchCampusRole role, SearchCampusIntent intent) {
        return state.snapshot().stream().filter(location -> location.role() == role.toEndpointRole())
                .anyMatch(location -> (intent == SearchCampusIntent.TEXT_ROUTE
                        ? location.intent() == ResolvedLocation.RouteIntent.TEXT_ROUTE
                        : location.intent() == ResolvedLocation.RouteIntent.NAVIGATE_ROUTE));
    }

    private void ensureTextRouteExecution(ResolvedLocationCollector state) {
        if (!state.hasTextRouteIntent() || state.hasRouteExecution()) {
            return;
        }
        FindRouteToolRequest request = state.currentTextRouteRequest();
        if (request == null) {
            return;
        }
        FindRouteToolResult result = campusToolAdapter.findRoute(request);
        state.recordRouteExecution(request, result);
    }

    private boolean hasSuccessfulTextRoute(RouteExecutionTrace trace) {
        return trace != null && trace.successful() && trace.route() != null
                && trace.start() != null && trace.end() != null
                && trace.start().intent() == ResolvedLocation.RouteIntent.TEXT_ROUTE
                && trace.end().intent() == ResolvedLocation.RouteIntent.TEXT_ROUTE;
    }

    private String deterministicTextRouteReply(RouteExecutionTrace trace) {
        StringBuilder reply = new StringBuilder(trace.start().name()).append("에서 ")
                .append(trace.end().name()).append("까지 경로를 찾았어요.");
        Long durationSeconds = trace.route().estimatedDurationSeconds();
        if (durationSeconds != null) {
            if (durationSeconds < 60) {
                reply.append(" 예상 소요 시간은 1분 이내예요.");
            } else {
                long minutes = Math.max(1L, Math.round(durationSeconds / 60.0));
                reply.append(" 예상 소요 시간은 약 ").append(minutes).append("분이에요.");
            }
        }
        reply.append(" 길찾기 화면으로 안내할까요?");
        return reply.toString();
    }
}
