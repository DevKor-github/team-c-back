package devkor.com.teamcback.domain.navigate.repository;

import devkor.com.teamcback.domain.navigate.entity.Edge;
import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdgeRepository extends JpaRepository<Edge, Long> {
    List<Edge> findByStartNode(Node startNode);
}
