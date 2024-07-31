package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

import java.util.List;

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
    private Double floor;
    private Double underFloor;
    private boolean isOperating;
    private String nextBuildingTime; // 현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환
    private List<FacilityType> facilityTypes;

    public GetBuildingDetailRes(Building building) {
        this.buildingId = building.getId();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.getNeedStudentCard();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.floor = building.getFloor();
        this.underFloor = building.getUnderFloor();
    }

    public GetBuildingDetailRes(Building building, List<FacilityType> facilityTypes) {
        this.buildingId = building.getId();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.getNeedStudentCard();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.floor = building.getFloor();
        this.underFloor = building.getUnderFloor();
        //TODO: 차후 isOperating, nextOperatingTime 수정하기(현재 임시값)
        this.isOperating = true;
        this.nextBuildingTime = "10:00";
        this.facilityTypes = facilityTypes;

    }
}
