package devkor.com.teamcback.domain.vote.repository;


import devkor.com.teamcback.domain.vote.dto.response.GetVoteOptionRes;

import java.util.List;

public interface CustomVoteRepository {
    List<GetVoteOptionRes> getVoteOptionsByPlaceByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId);
}
