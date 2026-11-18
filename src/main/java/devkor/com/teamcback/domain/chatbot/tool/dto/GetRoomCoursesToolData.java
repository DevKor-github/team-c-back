package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record GetRoomCoursesToolData(String roomName, Integer year, String term,
                                     List<RoomCourseToolItem> courses) {
}
