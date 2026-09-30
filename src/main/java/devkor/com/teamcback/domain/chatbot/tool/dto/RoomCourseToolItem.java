package devkor.com.teamcback.domain.chatbot.tool.dto;

import devkor.com.teamcback.domain.common.entity.Weekday;

public record RoomCourseToolItem(String subject, String professor, String courseCode, String section,
                                 Weekday weekday, int startPeriod, int endPeriod) {
}
