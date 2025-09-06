package devkor.com.teamcback.domain.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "장소에 대한 투표 정보")
@Getter
@AllArgsConstructor
public class SaveVoteRecordReq {
    @Schema(description = "장소 id", example = "5")
    private Long placeId;

    @Schema(description = "투표 주제 id", example = "1")
    private Long voteTopicId;

    @Schema(description = "투표 항목 id", example = "1")
    private Long voteOptionId;
}
