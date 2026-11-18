package devkor.com.teamcback.domain.chatbot.dto;

public record RoomCoursePlan(Intent intent, String roomQuery, String weekdayExpression) {
    public enum Intent { ROOM_COURSE, OTHER }
    public RoomCoursePlan { intent = intent == null ? Intent.OTHER : intent; }
    public boolean isRoomCourse() { return intent == Intent.ROOM_COURSE && roomQuery != null && !roomQuery.isBlank(); }
    public static RoomCoursePlan other() { return new RoomCoursePlan(Intent.OTHER, null, null); }
}
