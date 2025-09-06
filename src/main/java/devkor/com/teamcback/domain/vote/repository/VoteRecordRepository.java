package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    VoteRecord findByUserIdAndVoteTopicIdAndPlaceId(Long userId, Long voteTopicId, Long placeId);

    List<VoteRecord> findAllByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId);

    int countByVoteTopicIdAndVoteOptionId(Long voteTopicId, Long voteOptionId);

    int countByVoteTopicIdAndVoteOptionIdAndPlaceId(Long voteTopicId, Long voteOptionId, Long placeId);

    boolean existsByUserIdAndPlaceIdAndVoteTopicId(Long userId, Long placeId, Long voteTopicId);
}
