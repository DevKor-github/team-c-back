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
    private boolean isOperating;
    private Boolean needStudentCard;
    private Double longitude;
    private Double latitude;
    private Double floor;
    private Double underFloor;

    public GetBuildingDetailRes(Building building) {
        this.buildingId = building.getId();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.isOperating = building.isOperating();
        this.needStudentCard = building.isNeedStudentCard();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.floor = building.getFloor();
        this.underFloor = building.getUnderFloor();
    }
}
