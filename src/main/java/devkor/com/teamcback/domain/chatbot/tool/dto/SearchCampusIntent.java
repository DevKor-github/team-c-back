package devkor.com.teamcback.domain.chatbot.tool.dto;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent;

public enum SearchCampusIntent {
    NAVIGATE_ROUTE, TEXT_ROUTE;

    public RouteIntent toRouteIntent() {
        return RouteIntent.valueOf(name());
    }
}
