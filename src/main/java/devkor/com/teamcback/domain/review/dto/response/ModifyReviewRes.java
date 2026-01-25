package devkor.com.teamcback.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "리뷰 수정 응답 dto")
@Getter
public class ModifyReviewRes {
    private Long reviewId;

    public ModifyReviewRes(Long id) {
        this.reviewId = id;
    }
}
