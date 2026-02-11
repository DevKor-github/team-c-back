package devkor.com.teamcback.domain.vote.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Schema(description = "투표 내용 저장 완료")
@JsonIgnoreProperties
public class SaveVoteRecordRes implements ScoreUpdateResponse {
    @Setter
    @Schema(description = "레벨업 여부", example = "false")
    private boolean isLevelUp;
    @Setter
    @Schema(description = "현재 점수", example = "15")
    private Long currentScore;
}
