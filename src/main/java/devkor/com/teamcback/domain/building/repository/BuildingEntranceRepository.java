package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingEntrance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingEntranceRepository extends JpaRepository<BuildingEntrance, Long> {
    List<BuildingEntrance> findAllByBuilding(Building building);
}
