package devkor.com.teamcback.domain.chatbot.dto;

public record LocationDetailPlan(Intent intent, String locationQuery) {
    public enum Intent { LOCATION_DETAIL, OTHER }

    public LocationDetailPlan {
        intent = intent == null ? Intent.OTHER : intent;
    }

    public boolean isLocationDetail() {
        return intent == Intent.LOCATION_DETAIL && locationQuery != null && !locationQuery.isBlank();
    }

    public static LocationDetailPlan other() { return new LocationDetailPlan(Intent.OTHER, null); }
}
