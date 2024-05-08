package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
public class GlobalSearchRes {
    private Long id;
    private String name;
    private String address;
    private PlaceType placeType;

    public GlobalSearchRes(Building building, PlaceType placeType) {
        this.id = building.getId();
        this.name = building.getName();
        this.address = building.getAddress();
        this.placeType = placeType;
    }


    public GlobalSearchRes(Classroom classroom, PlaceType placeType) {
        this.id = classroom.getId();
        this.name = classroom.getBuilding().getName() + " " + classroom.getName();
        this.address = classroom.getBuilding().getAddress() + " " + classroom.getFloor() + "층";
        this.placeType = placeType;
    }

    public GlobalSearchRes(Facility facility, PlaceType placeType) {
        this.id = facility.getId();
        this.name = facility.getBuilding().getName() + " " + facility.getFloor() + "층 " + facility.getName();
        this.address = facility.getBuilding().getAddress() + " " + facility.getFloor() + "층";
        this.placeType = placeType;
    }
}
