package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record PlaceReviewsToolData(Long placeId, String placeName, Double averageRating,
                                   List<ReviewTagSummary> topTags, List<ReviewSummary> reviews,
                                   boolean truncated) {
}
