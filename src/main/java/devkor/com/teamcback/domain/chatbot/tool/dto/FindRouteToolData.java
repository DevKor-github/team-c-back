package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record FindRouteToolData(Long estimatedDurationSeconds, List<RouteStep> steps) {
}
