package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.routes.entity.Checkpoint;
import devkor.com.teamcback.domain.routes.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    Checkpoint findByNode(Node node);
}
