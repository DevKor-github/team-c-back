package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import java.util.List;

/** Request-local evidence that the textual route tool was executed. */
public record RouteExecutionTrace(
        ResolvedLocation start,
        ResolvedLocation end,
        List<RouteCondition> conditions,
        FindRouteToolData route,
        boolean successful) {
    public RouteExecutionTrace {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
