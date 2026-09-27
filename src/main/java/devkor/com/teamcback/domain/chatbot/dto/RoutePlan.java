package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import java.util.List;

/** Provider-neutral plan for the deterministic route workflow. */
public record RoutePlan(
        Intent intent,
        String startQuery,
        String endQuery,
        List<RouteCondition> conditions) {

    public RoutePlan {
        intent = intent == null ? Intent.NOT_ROUTE : intent;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public static RoutePlan notRoute() {
        return new RoutePlan(Intent.NOT_ROUTE, null, null, List.of());
    }

    public boolean isRoute() {
        return intent == Intent.TEXT_ROUTE || intent == Intent.NAVIGATE_ROUTE;
    }

    public boolean hasEndpointQueries() {
        return startQuery != null && !startQuery.isBlank()
                && endQuery != null && !endQuery.isBlank();
    }

    public enum Intent {
        TEXT_ROUTE, NAVIGATE_ROUTE, NOT_ROUTE
    }
}
