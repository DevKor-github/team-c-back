package devkor.com.teamcback.domain.vote.repository;


import devkor.com.teamcback.domain.vote.dto.response.GetVoteOptionRes;
import devkor.com.teamcback.domain.vote.entity.Vote;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;

import java.util.List;

public interface CustomVoteRepository {
    List<GetVoteOptionRes> getVoteOptionsByPlaceByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId);

    List<Vote> getVoteByStatusWithPage(VoteStatus status, Long voteTopicId, int size);

    List<GetVoteOptionRes> getVoteOptionByVoteId(Long voteId);

    List<GetVoteOptionRes> getVoteOptionByVoteIdOrderByCount(Long voteId);
}
