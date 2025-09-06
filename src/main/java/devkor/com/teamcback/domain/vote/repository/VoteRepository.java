package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId);
}
