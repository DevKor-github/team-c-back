package devkor.com.teamcback.domain.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SaveVoteRecordReq {
    @Schema(description = "투표 id", example = "1")
    private Long voteId;

    @Schema(description = "투표 옵션 id", example = "1")
    private Long voteOptionId;
}
