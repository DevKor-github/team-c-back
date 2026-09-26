package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import java.util.List;

public record ResolvedLocation(
        EndpointRole role,
        RouteEndpointType type,
        Long id,
        String name,
        RouteIntent intent,
        List<RouteCondition> conditions
) {
    public ResolvedLocation {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public enum EndpointRole {
        START, END
    }

    public enum RouteIntent {
        NAVIGATE_ROUTE, TEXT_ROUTE
    }
}
