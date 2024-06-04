package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchPlaceRes {
    private Long id;
    private String name;
    private String imageUrl;
    private String detail;
    private Integer floor;
    private String address;
    private String operatingTime;
    private Boolean needStudentCard;
    private Boolean plugAvailability;
    private Double longitude;
    private Double latitude;
    private Double xCoord;
    private Double yCoord;
    private PlaceType placeType;

    public SearchPlaceRes(Building building) {
        this.id = building.getId();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.getNeedStudentCard();
        this.longitude = building.getLongitude();
        this.latitude = building.getLatitude();
        this.placeType = PlaceType.BUILDING;
    }

    public SearchPlaceRes(Classroom classroom) {
        this.id = classroom.getId();
        this.name = classroom.getName();
        this.imageUrl = classroom.getImageUrl();
        this.detail = classroom.getDetail();
        this.floor = classroom.getFloor();
        this.plugAvailability = classroom.isPlugAvailability();
        this.longitude = classroom.getLongitude();
        this.latitude = classroom.getLatitude();
        this.xCoord = classroom.getNode().getXCoord();
        this.yCoord = classroom.getNode().getYCoord();
        this.placeType = PlaceType.CLASSROOM;
    }
}
