package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    VoteRecord findByUserIdAndVoteId(Long userId, Long voteId);

    boolean existsByUserIdAndVoteId(Long userId, Long voteId);
}
