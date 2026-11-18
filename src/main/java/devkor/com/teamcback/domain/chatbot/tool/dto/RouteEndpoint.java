package devkor.com.teamcback.domain.chatbot.tool.dto;

public record RouteEndpoint(RouteEndpointType type, Long locationId, Double latitude, Double longitude) {
}
