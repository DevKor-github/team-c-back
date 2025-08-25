package devkor.com.teamcback.domain.vote.repository;

import devkor.com.teamcback.domain.vote.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    List<VoteOption> findAllByVoteTopicId(Long id);
}
