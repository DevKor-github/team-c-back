package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Requests only a provider-neutral route plan; it never resolves IDs or executes a route. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class RoutePlanner {
    private final LlmGateway llmGateway;

    public RoutePlanner(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public RoutePlan plan(String systemPrompt, List<LlmGateway.ConversationMessage> history, String userMessage) {
        try {
            RoutePlan plan = llmGateway.planRoute(systemPrompt, history, userMessage);
            if (plan == null || !valid(plan)) {
                return RoutePlan.notRoute();
            }
            return plan;
        } catch (RuntimeException exception) {
            // A failed planner must not prevent ordinary information Tool Calling from proceeding.
            return RoutePlan.notRoute();
        }
    }

    private boolean valid(RoutePlan plan) {
        return !plan.isRoute() || plan.hasEndpointQueries();
    }
}
