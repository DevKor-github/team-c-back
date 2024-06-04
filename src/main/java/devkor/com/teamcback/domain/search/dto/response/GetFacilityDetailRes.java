package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

@Getter
public class GetFacilityDetailRes {
    private Long facilityId;
    private FacilityType type;
    private String name;
    private int floor;
    private String detail;
    private boolean availability;
    private boolean plugAvailability;
    private String imageUrl;
    private String operatingTime;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;

    public GetFacilityDetailRes(Facility facility) {
        this.facilityId = facility.getId();
        this.type = facility.getType();
        this.name = facility.getName();
        this.floor = facility.getFloor();
        this.detail = facility.getDetail();
        this.availability = facility.isAvailability();
        this.plugAvailability = facility.isPlugAvailability();
        this.imageUrl = facility.getImageUrl();
        this.operatingTime = facility.getImageUrl();
        this.longitude = facility.getLongitude();
        this.latitude = facility.getLatitude();
        this.xCoord = facility.getNode().getXCoord();
        this.yCoord = facility.getNode().getYCoord();
    }
}
