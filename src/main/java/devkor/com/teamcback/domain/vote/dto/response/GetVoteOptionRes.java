package devkor.com.teamcback.domain.vote.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "투표 항목 조회 결과")
@Getter
public class GetVoteOptionRes {

    @Schema(description = "투표 항목 ID", example = "1")
    private Long voteOptionId;

    @Schema(description = "투표 항목", example = "있다")
    private String optionText;

    @Schema(description = "투표수", example = "5")
    private int voteCount;

    public GetVoteOptionRes(Long voteOptionId, String optionText, int voteCount) {
        this.voteOptionId = voteOptionId;
        this.optionText = optionText;
        this.voteCount = voteCount;
    }
}
