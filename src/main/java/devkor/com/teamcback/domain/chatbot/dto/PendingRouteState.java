package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent;
import java.util.List;

/** Minimal typed state for a route interaction that needs another user turn. */
public record PendingRouteState(
        RouteIntent interactionType,
        PendingLocationRef resolvedStart,
        PendingLocationRef resolvedEnd,
        SearchCampusRole unresolvedRole,
        List<RouteCondition> conditions,
        List<PendingLocationRef> ambiguousCandidates
) {
    public PendingRouteState {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        ambiguousCandidates = ambiguousCandidates == null ? List.of() : List.copyOf(ambiguousCandidates);
    }
}
