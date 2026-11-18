package devkor.com.teamcback.domain.chatbot.tool.dto;

import devkor.com.teamcback.domain.common.entity.Weekday;

public record GetRoomCoursesToolRequest(Long placeId, Weekday weekday) {
}
