package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
public class AutoCompleteRes {
    private Long id;
    private String name;
    private PlaceType placeType;

    public AutoCompleteRes(Building building, PlaceType placeType) {
        this.id = building.getId();
        this.name = building.getName();
        this.placeType = placeType;
    }
}
