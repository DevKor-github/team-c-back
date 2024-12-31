package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.ConnectedBuilding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConnectedBuildingRepository extends JpaRepository<ConnectedBuilding, Long> {
    @Query("SELECT cb.connectedBuildingId FROM ConnectedBuilding cb WHERE cb.building = :building")
    List<Long> findConnectedBuildingsByBuilding(Building building);
}
