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
    private PlaceType type;
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FacilityType facilityType;
    private String name;
    private String detail;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean availability;  //사용 가능 여부
    private boolean plugAvailability;
    private String imageUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
        this.type = PlaceType.CLASSROOM;
        this.id = classroom.getId();
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
        this.maskIndex = classroom.getMaskIndex();
        this.bookmarked = bookmarked;
        //TODO: 나중에 운영 정보 수정해서 넣기
        this.isOperating = true;
        this.nextPlaceTime = "22:00";
    }

    public SearchPlaceDetailRes(Facility facility, boolean bookmarked) {
        this.type = PlaceType.FACILITY;
        this.id = facility.getId();
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
        //TODO: 나중에 운영 정보 수정해서 넣기
        this.isOperating = false;
        this.nextPlaceTime = "9:00";
    }
}
