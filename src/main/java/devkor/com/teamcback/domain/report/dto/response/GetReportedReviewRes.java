package devkor.com.teamcback.domain.report.dto.response;

import devkor.com.teamcback.domain.review.entity.Review;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
public class GetReportedReviewRes {
    private Long reviewId;
    private String comment;
    private Long placeId;
    private String placeName;
    private String reviewCreatedAt;
    private String reasonCategory;

    public GetReportedReviewRes(Review review, String reasonCategory) {
        this.reviewId = review.getId();
        this.placeId = review.getPlace().getId();
        this.placeName = review.getPlace().getName();
        this.comment = review.getComment();
        this.reasonCategory = reasonCategory;

        // 작성일 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd(E)", Locale.KOREAN);
        this.reviewCreatedAt = review.getCreatedAt() != null ? review.getCreatedAt().format(formatter): "";
    }
}
