package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Building findBuildingById(long id);

    Building findBuildingByNode(Node node);

    List<Building> findAllByIdNotIn(List<Long> buildingIdList);
}
