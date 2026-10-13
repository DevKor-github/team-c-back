package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;

/** Minimal, non-sensitive location reference used by pending interactions. */
public record PendingLocationRef(RouteEndpointType locationType, Long locationId, String name) {
}
