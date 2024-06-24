package devkor.com.teamcback.domain.navigate.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.entity.NodeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByBuildingAndRoutingAndTypeNot(Building building, boolean routing, NodeType type);
    List<Node> findByBuildingAndRouting(Building building, boolean routing);

}
