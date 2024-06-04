package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.facility.entity.Facility;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetFacilityRes {
    private Long facilityId;
    private String name;
    private Boolean availability;
    private String imageUrl;
    private Double xCoord;
    private Double yCoord;
    private Double longitude;
    private Double latitude;

    public GetFacilityRes(Facility facility) {
        this.facilityId = facility.getId();
        this.name = facility.getName();
        this.availability = facility.isAvailability();
        this.imageUrl = facility.getImageUrl();
        this.xCoord = facility.getNode().getXCoord();
        this.yCoord = facility.getNode().getYCoord();
        this.longitude = facility.getLongitude();
        this.latitude = facility.getLatitude();
    }
}
