package devkor.com.teamcback.domain.chatbot.dto;

public record FacilityPlan(Intent intent, String locationQuery, String facilityType, Integer floor) {
    public enum Intent { FACILITY, OTHER }
    public FacilityPlan { intent = intent == null ? Intent.OTHER : intent; }
    public boolean isFacility() { return intent == Intent.FACILITY; }
    public static FacilityPlan other() { return new FacilityPlan(Intent.OTHER, null, null, null); }
}
