package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;

/** Provider-neutral result of one request-local chatbot workflow. */
public record ChatOrchestrationResult(
        String reply,
        LlmGateway.CompletionStatus completionStatus,
        List<ResolvedLocation> resolvedLocations,
        List<SearchResolutionTrace> searchResolutions,
        RouteExecutionTrace routeExecution) {

    public ChatOrchestrationResult(String reply, List<ResolvedLocation> resolvedLocations) {
        this(reply, LlmGateway.CompletionStatus.COMPLETE, resolvedLocations, List.of(), null);
    }

    public ChatOrchestrationResult(String reply, List<ResolvedLocation> resolvedLocations,
                                   LlmGateway.CompletionStatus completionStatus) {
        this(reply, completionStatus, resolvedLocations, List.of(), null);
    }

    public ChatOrchestrationResult(String reply, List<ResolvedLocation> resolvedLocations,
                                   LlmGateway.CompletionStatus completionStatus,
                                   List<SearchResolutionTrace> searchResolutions) {
        this(reply, completionStatus, resolvedLocations, searchResolutions, null);
    }

    public ChatOrchestrationResult(String reply, List<ResolvedLocation> resolvedLocations,
                                   LlmGateway.CompletionStatus completionStatus,
                                   List<SearchResolutionTrace> searchResolutions,
                                   RouteExecutionTrace routeExecution) {
        this(reply, completionStatus, resolvedLocations, searchResolutions, routeExecution);
    }

    public ChatOrchestrationResult {
        completionStatus = completionStatus == null ? LlmGateway.CompletionStatus.COMPLETE : completionStatus;
        resolvedLocations = resolvedLocations == null ? List.of() : List.copyOf(resolvedLocations);
        searchResolutions = searchResolutions == null ? List.of() : List.copyOf(searchResolutions);
    }
}
