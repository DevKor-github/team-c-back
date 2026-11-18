package devkor.com.teamcback.domain.chatbot.dto;

/** Provider-neutral interpretation used only by the CROWD_STATUS proof of concept. */
public record CrowdStatusPlan(Intent intent, String locationQuery) {
    public CrowdStatusPlan {
        intent = intent == null ? Intent.OTHER : intent;
        locationQuery = locationQuery == null ? null : locationQuery.trim();
    }

    public static CrowdStatusPlan other() {
        return new CrowdStatusPlan(Intent.OTHER, null);
    }

    public boolean isCrowdStatus() {
        return intent == Intent.CROWD_STATUS && locationQuery != null && !locationQuery.isBlank();
    }

    public enum Intent {
        CROWD_STATUS, OTHER
    }
}
