package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Schema(description = "리뷰 삭제 응답 dto")
@Getter
public class DeleteReviewRes implements ScoreUpdateResponse {

    @Schema(description = "레벨업 여부")
    private boolean isLevelUp;

    @Schema(description = "현재 점수")
    private Long currentScore;

    @Schema(description = "점수 획득 여부 (삭제 시에는 항상 false)")
    private boolean scoreGained;
}
