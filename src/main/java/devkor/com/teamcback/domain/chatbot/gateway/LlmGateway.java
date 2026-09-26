package devkor.com.teamcback.domain.chatbot.gateway;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import java.util.List;

public interface LlmGateway {
    LlmResult generate(String systemPrompt, List<ConversationMessage> history, String userMessage);

    record LlmResult(String reply, List<ResolvedLocation> resolvedLocations, CompletionStatus completionStatus,
                     List<SearchResolutionTrace> searchResolutions, RouteExecutionTrace routeExecution) {
        public LlmResult(String reply, List<ResolvedLocation> resolvedLocations) {
            this(reply, resolvedLocations, CompletionStatus.COMPLETE, List.of(), null);
        }

        public LlmResult(String reply, List<ResolvedLocation> resolvedLocations, CompletionStatus completionStatus) {
            this(reply, resolvedLocations, completionStatus, List.of(), null);
        }

        public LlmResult(String reply, List<ResolvedLocation> resolvedLocations, CompletionStatus completionStatus,
                         List<SearchResolutionTrace> searchResolutions) {
            this(reply, resolvedLocations, completionStatus, searchResolutions, null);
        }

        public LlmResult {
            resolvedLocations = resolvedLocations == null ? List.of() : List.copyOf(resolvedLocations);
            completionStatus = completionStatus == null ? CompletionStatus.COMPLETE : completionStatus;
            searchResolutions = searchResolutions == null ? List.of() : List.copyOf(searchResolutions);
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
