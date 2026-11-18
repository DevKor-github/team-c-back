package devkor.com.teamcback.domain.chatbot.dto;

public record PlaceReviewsPlan(Intent intent, String locationQuery) {
    public enum Intent { PLACE_REVIEWS, OTHER }

    public PlaceReviewsPlan {
        intent = intent == null ? Intent.OTHER : intent;
    }

    public boolean isPlaceReviews() {
        return intent == Intent.PLACE_REVIEWS && locationQuery != null && !locationQuery.isBlank();
    }

    public static PlaceReviewsPlan other() { return new PlaceReviewsPlan(Intent.OTHER, null); }
}
