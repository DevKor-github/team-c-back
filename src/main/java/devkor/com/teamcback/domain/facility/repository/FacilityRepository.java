package devkor.com.teamcback.domain.facility.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;

import devkor.com.teamcback.domain.search.entity.PlaceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    boolean existsByBuildingAndType(Building building, FacilityType type);

    List<Facility> findByNameContaining(String word);

    List<Facility> findAllByBuildingAndType(Building building, FacilityType type);

    List<Facility> findAllByBuilding(Building building);

    List<Facility> findAllByBuildingAndFloor(Building building, double floor);

    List<Facility> findAllByType(FacilityType facilityType);

    Facility findByNode(Node node);

    Facility findByBuildingAndFloorAndMaskIndex(Building building, double floor, Integer maskIndex);

    List<Facility> findAllByTypeInOrderByFloor(List<FacilityType> types);
}
