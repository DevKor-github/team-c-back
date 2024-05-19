package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalSearchRes {
    private Long id;
    private String name;
    private Integer floor;
    private String address;
    private Double longitude;
    private Double latitude;
    private PlaceType placeType;

    public GlobalSearchRes(Building building, PlaceType placeType) {
        this.id = building.getId();
        this.name = building.getName();
        this.address = building.getAddress();
        this.longitude = building.getLongitude();
        this.latitude = building.getLatitude();
        this.placeType = placeType;
    }

    public GlobalSearchRes(Classroom classroom, PlaceType placeType) {
        this.id = classroom.getId();
        this.name = classroom.getBuilding().getName() + " " + classroom.getName();
        this.floor = classroom.getFloor();
        this.longitude = classroom.getLongitude();
        this.latitude = classroom.getLatitude();
        this.placeType = placeType;
    }

    public GlobalSearchRes(FacilityType facilityType, PlaceType placeType) {
        this.name = facilityType.getName();
        this.placeType = placeType;
    }
}
