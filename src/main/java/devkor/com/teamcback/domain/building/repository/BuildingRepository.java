package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Building findBuildingByNode(Node node);
}
