package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record FindRouteToolRequest(RouteEndpoint start, RouteEndpoint end, List<RouteCondition> conditions) {
}
