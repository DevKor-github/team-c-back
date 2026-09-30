package devkor.com.teamcback.domain.chatbot.tool.dto;

public record RouteStep(RouteSectionType sectionType, Long buildingId, Double floor, String instruction) {
}
