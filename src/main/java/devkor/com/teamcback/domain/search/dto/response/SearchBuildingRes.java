package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "건물 목록 조회 시 건물 조회")
public class SearchBuildingRes {
    private Long buildingId;
    private String name;
    private String imageUrl;
    private String detail;
    private String address;
    private String operatingTime;
    private boolean isOperating;
    private Boolean needStudentCard;
    private Double longitude;
    private Double latitude;
    private Double floor;
    private Double underFloor;
    private String nextBuildingTime; // 현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FacilityType> facilityTypes;

    public SearchBuildingRes(Building building, List<FacilityType> facilityTypes) {
        this.buildingId = building.getId();
        this.name = "고려대학교 서울캠퍼스 " + building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.isNeedStudentCard();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.floor = building.getFloor();
        this.underFloor = building.getUnderFloor();
        this.facilityTypes = facilityTypes;
        this.isOperating = building.isOperating();
        if(building.isOperating()) { // 운영 중이면 종료 시간
            this.nextBuildingTime = building.getOperatingTime().substring(6);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextBuildingTime = building.getOperatingTime().substring(0, 5);
        }
    }
}
