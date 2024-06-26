package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

@Getter
@JsonInclude(Include.NON_NULL)
public class GetRoomDetailRes {
    private String type;
    private Long id;
    private FacilityType facilityType;
    private String name;
    private String detail;
    private boolean availability;
    private boolean plugAvailability;
    private String imageUrl;
    private String operatingTime;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;

    public GetRoomDetailRes(Classroom classroom) {
        this.type = "CLASSROOM";
        this.id = classroom.getId();
        this.name = classroom.getName();
        this.detail = classroom.getDetail();
        this.plugAvailability = classroom.isPlugAvailability();
        this.imageUrl = classroom.getImageUrl();
        this.longitude = classroom.getNode().getLongitude();
        this.latitude = classroom.getNode().getLatitude();
        this.xCoord = classroom.getNode().getXCoord();
        this.yCoord = classroom.getNode().getYCoord();
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
        this.operatingTime = facility.getImageUrl();
        this.longitude = facility.getNode().getLongitude();
        this.latitude = facility.getNode().getLatitude();
        this.xCoord = facility.getNode().getXCoord();
        this.yCoord = facility.getNode().getYCoord();
    }
}
