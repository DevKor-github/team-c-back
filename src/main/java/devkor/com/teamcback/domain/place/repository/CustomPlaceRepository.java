package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;

import java.util.List;

public interface CustomPlaceRepository {
    List<Place> getFacilitiesByBuildingAndTypesWithPage(Long buildingId, List<PlaceType> mainFacilityTypes, Place lastPlace, int size);
}
