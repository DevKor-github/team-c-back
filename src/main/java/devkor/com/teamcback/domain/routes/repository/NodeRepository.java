package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByBuildingAndRoutingAndTypeNot(Building building, boolean routing, NodeType type);
    List<Node> findByBuildingAndRouting(Building building, boolean routing);
    List<Node> findAllByBuildingAndFloor(Building building, Double floor);
    List<Node> findAllByBuildingAndType(Building building, NodeType nodeType);
    List<Node> findAllByBuildingAndFloorAndTypeIn(Building building, double floor, List<NodeType> entrance);
}
