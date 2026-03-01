package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.domain.review.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Getter
public class GetReviewRes {
    @Schema(description = "리뷰 id")
    private Long reviewId;
    @Schema(description = "리뷰 사용자 fineUuid")
    private String fileUuid;
    @Schema(description = "별점")
    private double score;
    @Schema(description = "재방문 여부")
    private boolean isRevisit;
    @Schema(description = "한줄평")
    private String comment;
    @Schema(description = "리뷰 장소 id")
    private Long placeId;
    @Schema(description = "리뷰 사용자 id")
    private Long userId;
    @Schema(description = "리뷰 작성일")
    private String createdAt;
    @Schema(description = "리뷰 태그 목록")
    private List<GetReviewTagRes> tagList;
    @Schema(description = "리뷰 사진 목록")
    private List<SearchReviewImageRes> reviewImageList;

    public GetReviewRes(Review review, List<GetReviewTagRes> tagList, List<SearchReviewImageRes> reviewImageRes) {
        this.userId = review.getUser() != null ? review.getUser().getUserId() : null;
        this.reviewId = review.getId();
        this.placeId = review.getPlace().getId();
        this.score = review.getScore();
        this.fileUuid = review.getFileUuid();
        this.isRevisit = review.isRevisit();
        this.comment = review.getComment();

        // 작성일 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd(E)", Locale.KOREAN);
        this.createdAt = review.getCreatedAt() != null ? review.getCreatedAt().format(formatter): "";

        // 리뷰 태그
        this.tagList = tagList;

        // 리뷰 사진
        this.reviewImageList = reviewImageRes;
    }
}
