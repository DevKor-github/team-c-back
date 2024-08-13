package devkor.com.teamcback.domain.admin.facility.dto.response;

import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "편의시설 응답 dto")
@Getter
public class GetFacilityRes {
    private Long facilityId;
    private Long nodeId;
    private String detail;
    private String name;
    private FacilityType type;
    private String operatingTime;
    private String imageUrl;
    private boolean availability;
    private boolean plugAvailability;
    private boolean isOperating;
    private Integer maskIndex;

    public GetFacilityRes(Facility facility) {
        this.facilityId = facility.getId();
        this.nodeId = facility.getNode().getId();
        this.detail = facility.getDetail();
        this.name = facility.getName();
        this.type = facility.getType();
        this.operatingTime = facility.getOperatingTime();
        this.imageUrl = facility.getImageUrl();
        this.availability = facility.isAvailability();
        this.plugAvailability = facility.isPlugAvailability();
        this.isOperating = facility.isOperating();
        this.maskIndex = facility.getMaskIndex();
    }

}
