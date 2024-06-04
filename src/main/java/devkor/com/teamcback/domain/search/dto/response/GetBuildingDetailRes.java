package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import lombok.Getter;

@Getter
public class GetBuildingDetailRes {
    private Long buildingId;
    private String name;
    private String imageUrl;
    private String detail;
    private String address;
    private String operatingTime;
    private Boolean needStudentCard;
    private Double longitude;
    private Double latitude;

    public GetBuildingDetailRes(Building building) {
        this.buildingId = building.getId();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.getNeedStudentCard();
        this.longitude = building.getLongitude();
        this.latitude = building.getLatitude();
    }
}
