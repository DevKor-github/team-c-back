package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    VoteRecord findByUserIdAndVoteTopicIdAndVoteOptionIdAndPlaceId(Long userId, Long voteTopicId, Long voteOptionId, Long placeId);
}
