package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import java.util.List;

/** Request-local trace of searchCampus, including ambiguous candidates. */
public record SearchResolutionTrace(
        SearchCampusRole role,
        SearchCampusIntent intent,
        String query,
        boolean ambiguous,
        List<PendingLocationRef> candidates,
        List<RouteCondition> conditions
) {
    public SearchResolutionTrace {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
