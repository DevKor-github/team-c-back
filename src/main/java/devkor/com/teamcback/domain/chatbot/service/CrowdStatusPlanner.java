package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.CrowdStatusPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Keeps crowd intent/slot extraction separate from crowd capability execution. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CrowdStatusPlanner {
    private static final String PROMPT = """
            Interpret only whether the user asks for current campus crowd/congestion status.
            Return CROWD_STATUS with the natural-language locationQuery for a crowd question;
            otherwise return OTHER. Do not resolve IDs, types, roles, conditions, or tool arguments.
            """;

    private final LlmGateway gateway;

    public CrowdStatusPlanner(LlmGateway gateway) {
        this.gateway = gateway;
    }

    public CrowdStatusPlan plan(List<LlmGateway.ConversationMessage> history, String userMessage) {
        CrowdStatusPlan result = gateway.planCrowd(PROMPT, history == null ? List.of() : history, userMessage);
        return result == null ? CrowdStatusPlan.other() : result;
    }
}
