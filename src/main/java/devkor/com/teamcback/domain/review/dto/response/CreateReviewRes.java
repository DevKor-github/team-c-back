package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "리뷰 생성 응답 dto")
@Getter
public class CreateReviewRes implements ScoreUpdateResponse {
    private Long reviewId;

    @Setter
    @Schema(description = "레벨업 여부")
    private boolean isLevelUp;

    @Setter
    @Schema(description = "현재 점수")
    private Long currentScore;

    @Setter
    @Schema(description = "점수 획득 여부")
    private boolean scoreGained;

    public CreateReviewRes(Long id) {
        this.reviewId = id;
    }
}
