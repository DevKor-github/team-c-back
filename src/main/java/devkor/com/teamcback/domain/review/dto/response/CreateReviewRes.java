package devkor.com.teamcback.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "리뷰 생성 응답 dto")
@Getter
public class CreateReviewRes {
    private Long reviewId;

    public CreateReviewRes(Long id) {
        this.reviewId = id;
    }
}
