package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

@Getter
public class GetRoomDetailRes {
    private String type;
    private Long id;
    @JsonInclude(Include.NON_NULL)
    private FacilityType facilityType;
    private String name;
    private String detail;
    @JsonInclude(Include.NON_NULL)
    private boolean availability;
    private boolean plugAvailability;
    private String imageUrl;
    private String operatingTime;
    private boolean isOperating;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;
    private Integer maskIndex;

    public GetRoomDetailRes(Classroom classroom) {
        this.type = "CLASSROOM";
        this.id = classroom.getId();
        this.name = classroom.getName();
        this.detail = classroom.getDetail();
        this.plugAvailability = classroom.isPlugAvailability();
        this.imageUrl = classroom.getImageUrl();
        this.operatingTime = classroom.getOperatingTime();
        this.isOperating = classroom.isOperating();
        this.longitude = classroom.getNode().getLongitude();
        this.latitude = classroom.getNode().getLatitude();
        this.xCoord = classroom.getNode().getXCoord();
        this.yCoord = classroom.getNode().getYCoord();
        this.maskIndex = classroom.getMaskIndex();
    }

    public GetRoomDetailRes(Facility facility) {
        this.type = "FACILITY";
        this.id = facility.getId();
        this.facilityType = facility.getType();
        this.name = facility.getName();
        this.detail = facility.getDetail();
        this.availability = facility.isAvailability();
        this.plugAvailability = facility.isPlugAvailability();
        this.imageUrl = facility.getImageUrl();
        this.operatingTime = facility.getOperatingTime();
        this.isOperating = facility.isOperating();
        this.longitude = facility.getNode().getLongitude();
        this.latitude = facility.getNode().getLatitude();
        this.xCoord = facility.getNode().getXCoord();
        this.yCoord = facility.getNode().getYCoord();
        this.maskIndex = facility.getMaskIndex();
    }
}
