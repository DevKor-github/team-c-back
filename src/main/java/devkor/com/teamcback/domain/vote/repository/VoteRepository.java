package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.Vote;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId);

    @Query("SELECT v FROM Vote v WHERE v.status IS NULL OR v.status = :status")
    List<Vote> findAllByStatus(@Param("status")VoteStatus status);
}
