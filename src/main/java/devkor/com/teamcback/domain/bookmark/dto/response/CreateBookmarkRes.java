package devkor.com.teamcback.domain.bookmark.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties
public class CreateBookmarkRes implements ScoreUpdateResponse {
    @Setter
    @Schema(description = "레벨업 여부", example = "false")
    private boolean isLevelUp;
    @Setter
    @Schema(description = "현재 점수", example = "15")
    private Long currentScore;
    @Setter
    @Schema(description = "점수 획득 여부", example = "true")
    private boolean scoreGained;
}
