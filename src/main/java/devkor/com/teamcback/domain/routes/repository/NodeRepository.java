package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByBuildingAndRoutingAndTypeNot(Building building, boolean routing, NodeType type);
    List<Node> findByBuildingAndRouting(Building building, boolean routing);
    List<Node> findAllByBuildingAndFloor(Building building, Double floor);
    List<Node> findAllByBuildingAndType(Building building, NodeType nodeType);
    List<Node> findAllByBuildingAndFloorAndTypeIn(Building building, double floor, List<NodeType> entrance);
    List<Node> findByBuildingAndRoutingAndTypeIn(Building building, boolean routing, List<NodeType> type);
    @Query("SELECT e FROM Node e ORDER BY FUNCTION('RAND') LIMIT 20")
    List<Node> findRandomNodes();
    @Query("SELECT e FROM Node e WHERE e.building = :building ORDER BY FUNCTION('RAND') LIMIT 2")
    List<Node> findRandomNodesByBuilding(@Param("building") Building building);
}
