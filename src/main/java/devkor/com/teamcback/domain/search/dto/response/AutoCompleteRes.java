package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
public class AutoCompleteRes {
    private Long id;
    private String name;
    private String address;
    private PlaceType placeType;

    public AutoCompleteRes(Building building, PlaceType placeType) {
        this.id = building.getId();
        this.name = building.getName();
        this.address = building.getAddress();
        this.placeType = placeType;
    }


    public AutoCompleteRes(Classroom classroom, PlaceType placeType) {
        this.id = classroom.getId();
        this.name = classroom.getBuilding().getName() + " " + classroom.getName();
        this.address = classroom.getBuilding().getAddress() + " " + classroom.getFloor() + "층";
        this.placeType = placeType;
    }
}
