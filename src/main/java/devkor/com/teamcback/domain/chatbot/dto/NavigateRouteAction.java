package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import java.util.List;

public record NavigateRouteAction(
        RouteEndpointType startType,
        Long startId,
        String startName,
        RouteEndpointType endType,
        Long endId,
        String endName,
        List<RouteCondition> conditions
) {
    public NavigateRouteAction {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
