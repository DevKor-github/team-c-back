package devkor.com.teamcback.domain.chatbot.gateway;

import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import java.util.List;

public interface LlmGateway {
    /** Structured route interpretation; provider implementations must not resolve IDs or execute routes. */
    default RoutePlan planRoute(String systemPrompt, List<ConversationMessage> history, String userMessage) {
        return RoutePlan.notRoute();
    }

    LlmResult generate(String systemPrompt, List<ConversationMessage> history, String userMessage,
                       ResolvedLocationCollector executionState);

    record LlmResult(String reply, CompletionStatus completionStatus) {
        public LlmResult(String reply) {
            this(reply, CompletionStatus.COMPLETE);
        }

        public LlmResult {
            completionStatus = completionStatus == null ? CompletionStatus.COMPLETE : completionStatus;
        }
    }

    enum CompletionStatus {
        COMPLETE,
        FAILED_AFTER_TOOL_EXECUTION
    }

    enum Role {
        USER,
        ASSISTANT
    }

    record ConversationMessage(Role role, String content) {
    }
}
