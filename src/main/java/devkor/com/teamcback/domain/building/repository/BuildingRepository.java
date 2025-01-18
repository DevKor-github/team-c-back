package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    @Query(value = "SELECT * FROM tb_building WHERE node_id IN :nodeIds", nativeQuery = true)
    Optional<Building> findByNodeIdIn(@Param("nodeIds") Long[] nodeIds);
}
