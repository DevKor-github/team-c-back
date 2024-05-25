package devkor.com.teamcback.domain.navigate.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByBuildingAndFloorAndRouting(Building building, int floor, boolean routing);
}
