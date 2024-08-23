package devkor.com.teamcback.domain.navigate.repository;

import devkor.com.teamcback.domain.navigate.entity.Checkpoint;
import devkor.com.teamcback.domain.navigate.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    Checkpoint findByNode(Node node);
}
