package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.common.PlaceType;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchPlaceDetailRes {
    private Long buildingId;
    private int floor;
    private PlaceType type;
    private Long placeId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FacilityType facilityType;
    private String name;
    private String detail;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean availability;  //사용 가능 여부
    private boolean plugAvailability;
    private String imageUrl;
    private String operatingTime;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;
    private Integer maskIndex;
    private boolean bookmarked;
    private boolean isOperating;
    private String nextPlaceTime;

    public SearchPlaceDetailRes(Classroom classroom, boolean bookmarked) {
        this.buildingId = classroom.getBuilding().getId();
        this.floor = classroom.getFloor().intValue();
        this.type = PlaceType.CLASSROOM;
        this.placeId = classroom.getId();
        if(classroom.getBuilding().getId() == 0) {
            this.name = classroom.getName();
            this.longitude = classroom.getNode().getLongitude();
            this.latitude = classroom.getNode().getLatitude();
        } else {
            this.name = classroom.getBuilding().getName() + " " + classroom.getName();
            this.xCoord = classroom.getNode().getXCoord();
            this.yCoord = classroom.getNode().getYCoord();
        }
        this.detail = classroom.getDetail();
        this.plugAvailability = classroom.isPlugAvailability();
        this.imageUrl = classroom.getImageUrl();
        this.operatingTime = classroom.getOperatingTime();
        this.maskIndex = classroom.getMaskIndex();
        this.bookmarked = bookmarked;
        this.isOperating = classroom.isOperating();
        if(classroom.isOperating()) { // 운영 중이면 종료 시간
            this.nextPlaceTime = classroom.getOperatingTime().substring(6);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextPlaceTime = classroom.getOperatingTime().substring(0, 5);
        }
    }

    public SearchPlaceDetailRes(Facility facility, boolean bookmarked) {
        this.buildingId = facility.getBuilding().getId();
        this.floor = facility.getFloor().intValue();
        this.type = PlaceType.FACILITY;
        this.placeId = facility.getId();
        this.facilityType = facility.getType();
        if(facility.getBuilding().getId() == 0) {
            this.name = facility.getName();
            this.longitude = facility.getNode().getLongitude();
            this.latitude = facility.getNode().getLatitude();
        } else {
            this.name = facility.getBuilding().getName() + " " + facility.getName();
            this.xCoord = facility.getNode().getXCoord();
            this.yCoord = facility.getNode().getYCoord();
        }
        this.detail = facility.getDetail();
        this.availability = facility.isAvailability();
        this.plugAvailability = facility.isPlugAvailability();
        this.imageUrl = facility.getImageUrl();
        this.operatingTime = facility.getOperatingTime();
        this.maskIndex = facility.getMaskIndex();
        this.bookmarked = bookmarked;
        this.isOperating = facility.isOperating();
        if(facility.isOperating()) { // 운영 중이면 종료 시간
            this.nextPlaceTime = facility.getOperatingTime().substring(6);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextPlaceTime = facility.getOperatingTime().substring(0, 5);
        }
    }
}
