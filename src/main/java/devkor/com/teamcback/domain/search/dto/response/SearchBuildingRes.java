package devkor.com.teamcback.domain.search.dto.response;

import static devkor.com.teamcback.domain.operatingtime.service.OperatingService.OPERATING_TIME_PATTERN;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
@Schema(description = "건물 목록 조회 시 건물 조회")
public class SearchBuildingRes {
    private Long buildingId;
    private String name;
    private String imageUrl;
    private String detail;
    private String address;
    private String weekdayOperatingTime;
    private String saturdayOperatingTime;
    private String sundayOperatingTime;
    private boolean isOperating;
    private Boolean needStudentCard;
    private Double longitude;
    private Double latitude;
    private Double floor;
    private Double underFloor;
    private String nextBuildingTime; // 현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PlaceType> placeTypes;

    public SearchBuildingRes(Building building, List<PlaceType> placeTypes) {
        this.buildingId = building.getId();
        this.name = "고려대학교 서울캠퍼스 " + building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.weekdayOperatingTime = building.getWeekdayOperatingTime();
        this.saturdayOperatingTime = building.getSaturdayOperatingTime();
        this.sundayOperatingTime = building.getSundayOperatingTime();
        this.needStudentCard = building.isNeedStudentCard();
        if(building.getId() != 0) {
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        }
        this.floor = building.getFloor();
        this.underFloor = building.getUnderFloor();
        this.placeTypes = placeTypes;
        this.isOperating = building.isOperating();
        if(building.getOperatingTime() == null || !Pattern.matches(OPERATING_TIME_PATTERN, building.getOperatingTime())) {
            this.nextBuildingTime = null;
        }
        else if(building.isOperating()) { // 운영 중이면 종료 시간
            this.nextBuildingTime = building.getOperatingTime().substring(6, 11);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextBuildingTime = building.getOperatingTime().substring(0, 5);
        }
    }
}
