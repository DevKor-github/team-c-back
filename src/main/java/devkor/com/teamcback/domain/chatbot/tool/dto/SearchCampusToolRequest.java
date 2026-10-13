package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record SearchCampusToolRequest(
        String query,
        Integer limit,
        SearchCampusRole role,
        SearchCampusIntent intent,
        List<RouteCondition> conditions
) {
    public SearchCampusToolRequest(String query, Integer limit) {
        this(query, limit, null, null, List.of());
    }

    public SearchCampusToolRequest {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
