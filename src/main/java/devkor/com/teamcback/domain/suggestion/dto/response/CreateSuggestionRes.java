package devkor.com.teamcback.domain.suggestion.dto.response;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "건의 생성 완료")
@Getter
public class CreateSuggestionRes implements ScoreUpdateResponse {
    private Long suggestionId;
    @Setter
    @Schema(description = "레벨업 여부", example = "false")
    private boolean isLevelUp;
    @Setter
    @Schema(description = "현재 점수", example = "15")
    private Long currentScore;
    @Setter
    @Schema(description = "점수 획득 여부", example = "true")
    private boolean scoreGained;

    public CreateSuggestionRes(Suggestion suggestion) {
        this.suggestionId = suggestion.getId();
    }
}
