package devkor.com.teamcback.domain.vote.dto.response;

import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "투표 목록 조회 결과")
@Getter
public class GetVoteRes {

    @Schema(description = "투표 주제 ID", example = "1")
    private Long voteTopicId;

    @Schema(description = "투표 주제", example = "콘센트 유무")
    private String topic;

    @Schema(description = "투표 상태", example = "종료")
    private String status;

    @Schema(description = "투표 항목 리스트")
    private List<GetVoteOptionRes> optionList;

    public GetVoteRes(Long voteTopicId, String topic, String status, List<GetVoteOptionRes> optionList) {
        this.voteTopicId = voteTopicId;
        this.topic = topic;
        this.status = status;
        this.optionList = optionList;
    }
}
