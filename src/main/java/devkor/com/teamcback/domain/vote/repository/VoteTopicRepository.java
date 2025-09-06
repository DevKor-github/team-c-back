package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteTopic;
import org.springframework.data.jpa.repository.JpaRepository;


public interface VoteTopicRepository extends JpaRepository<VoteTopic, Long> {
}
