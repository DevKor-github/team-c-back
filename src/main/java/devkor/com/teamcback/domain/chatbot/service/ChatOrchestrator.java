package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Owns one message's request-local state and deterministic workflow invariants. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatOrchestrator {
    private final LlmGateway llmGateway;
    private final CampusToolAdapter campusToolAdapter;

    public ChatOrchestrator(LlmGateway llmGateway, CampusToolAdapter campusToolAdapter) {
        this.llmGateway = llmGateway;
        this.campusToolAdapter = campusToolAdapter;
    }

    public ChatOrchestrationResult execute(String systemPrompt, List<LlmGateway.ConversationMessage> history,
                                           String userMessage) {
        ResolvedLocationCollector state = new ResolvedLocationCollector();
        LlmGateway.LlmResult llmResult = llmGateway.generate(systemPrompt, history, userMessage, state);

        ensureTextRouteExecution(state);
        String reply = llmResult.reply();
        if ((reply == null || reply.isBlank()) && hasSuccessfulTextRoute(state.routeExecution())) {
            reply = deterministicTextRouteReply(state.routeExecution());
        }

        return new ChatOrchestrationResult(reply, llmResult.completionStatus(), state.snapshot(),
                state.searchResolutionSnapshot(), state.routeExecution());
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
                .append(trace.end().name()).append("까지 경로를 찾았습니다.");
        if (trace.route().estimatedDurationSeconds() != null) {
            reply.append(" 예상 소요 시간은 ").append(trace.route().estimatedDurationSeconds()).append("초입니다.");
        }
        return reply.toString();
    }
}
