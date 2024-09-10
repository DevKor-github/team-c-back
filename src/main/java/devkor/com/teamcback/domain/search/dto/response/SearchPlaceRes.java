package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "건물 및 강의실 조회 결과")
@Getter
@NoArgsConstructor
public class SearchPlaceRes {
    @Schema(description = "건물 또는 강의실 id", example = "1")
    private Long id;
    @Schema(description = "건물 id", example = "1")
    private Long buildingId;
    @Schema(description = "건물명", example = "애기능생활관")
    private String buildingName;
    @Schema(description = "건물 또는 강의실 이름", example = "애기능생활관")
    private String name;
    @Schema(description = "건물 또는 강의실 image url", example = "주소")
    private String imageUrl;
    @Schema(description = "건물 또는 강의실 설명", example = "설명")
    private String detail;
    @Schema(description = "강의실 층수", example = "3")
    private Double floor;
    @Schema(description = "건물 주소", example = "서울특별시 성북구 안암로 145 고려대학교 애기능생활관")
    private String address;
    @Schema(description = " 평일 운영 시간", example = "9:00-22:00")
    private String weekdayOperatingTime;
    @Schema(description = " 토요일 운영 시간", example = "9:00-22:00")
    private String saturdayOperatingTime;
    @Schema(description = " 일요일 운영 시간", example = "9:00-22:00")
    private String sundayOperatingTime;
    @Schema(description = "건물 운영 여부", example = "true")
    private boolean isOperating;
    @Schema(description = "건물 출입 시 학생증 필요 여부", example = "false")
    private Boolean needStudentCard;
    @Schema(description = "강의실 전기 콘센트 유무", example = "true")
    private Boolean plugAvailability;
    @Schema(description = "건물 또는 강의실 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "건물 또는 강의실 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "강의실 내부지도 상 x좌표", example = "100")
    private Double xCoord;
    @Schema(description = "강의실 내부지도 상 y좌표", example = "250")
    private Double yCoord;
    @Schema(description = "건물 또는 강의실 종류", example = "BUILDING")
    private LocationType locationType;
    @Schema(description = "편의시설 부가 설명", example = "문의: xx-xxx-xxxx")
    private String description;
    @Schema(description = "편의시설 별점", example = "NaN 또는 3.6666666666666665")
    private double starAverage;

    public SearchPlaceRes(Place place) {
        this.id = place.getId();
        this.buildingId = place.getBuilding().getId();
        this.buildingName = place.getBuilding().getName();
        this.name = place.getName();
        this.imageUrl = place.getImageUrl();
        this.detail = place.getDetail();
        this.weekdayOperatingTime = place.getWeekdayOperatingTime();
        this.saturdayOperatingTime = place.getSaturdayOperatingTime();
        this.sundayOperatingTime = place.getSundayOperatingTime();
        this.isOperating = place.isOperating();
        this.floor = place.getFloor();
        this.plugAvailability = place.isPlugAvailability();
        this.longitude = place.getNode().getLongitude();
        this.latitude = place.getNode().getLatitude();
        this.xCoord = place.getNode().getXCoord();
        this.yCoord = place.getNode().getYCoord();
        this.locationType = LocationType.PLACE;
        this.description = place.getDescription();
        this.starAverage = ((double) place.getStarSum()) / place.getStarNum();
    }
}
