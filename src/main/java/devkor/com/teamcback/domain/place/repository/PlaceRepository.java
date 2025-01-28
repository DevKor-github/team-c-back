package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.routes.entity.Node;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByBuildingAndType(Building building, PlaceType type);

    List<Place> findAllByBuildingAndTypeIn(Building building, List<PlaceType> types);

    List<Place> findAllByBuilding(Building building);

    List<Place> findAllByBuildingAndFloor(Building building, double floor);

    List<Place> findAllByType(PlaceType placeType);

    List<Place> findAllByTypeIn(List<PlaceType> types);

    Place findByNode(Node node);

    Place findByBuildingAndFloorAndMaskIndex(Building building, double floor, Integer maskIndex);

    List<Place> findAllByBuildingAndTypeInOrderByFloor(Building building, List<PlaceType> types);
}
