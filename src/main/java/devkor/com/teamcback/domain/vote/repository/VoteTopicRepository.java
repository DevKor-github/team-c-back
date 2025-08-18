package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import devkor.com.teamcback.domain.vote.entity.VoteTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteTopicRepository extends JpaRepository<VoteTopic, Long> {
    @Query("SELECT v FROM VoteTopic v WHERE (:status IS NULL OR v.status = :status)")
    List<VoteTopic> findAllByStatus(@Param("status") VoteStatus status);
}
