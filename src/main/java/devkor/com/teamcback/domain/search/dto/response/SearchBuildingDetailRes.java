package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.regex.Pattern;

import static devkor.com.teamcback.domain.operatingtime.service.OperatingService.OPERATING_TIME_PATTERN;

@Getter
@Schema(description = "건물 상세 정보")
public class SearchBuildingDetailRes {
    @Schema(description = "건물 id", example = "1")
    private Long buildingId;
    @Schema(description = "건물명", example = "애기능생활관")
    private String name;
    @Schema(description = "주소", example = "서울 성북구 안암로 73-15")
    private String address;
    @Schema(description = "건물 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "건물 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "건물 사진 url", example = "building_url")
    private String imageUrl;
    @Schema(description = " 평일 운영 시간", example = "9:00-22:00")
    private String weekdayOperatingTime;
    @Schema(description = " 토요일 운영 시간", example = "9:00-22:00")
    private String saturdayOperatingTime;
    @Schema(description = " 일요일 운영 시간", example = "9:00-22:00")
    private String sundayOperatingTime;
    @Schema(description = "건물 정보(TMI)", example = "애기능생활관이다.")
    private String details;
    @Schema(description = "북마크 저장 여부", example = "false")
    private boolean bookmarked;
    @Schema(description = "건물 내 시설 종류", example = "LOUNGE, GYM, ...")
    private List<PlaceType> existTypes; //건물 내 facility 종류 리스트(아이콘용)
    @Schema(description = "운영 여부", example = "false")
    private boolean isOperating;
    @Schema(description = "현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환", example = "9:00")
    private String nextBuildingTime; // 현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환
    @Schema(description = "주요 시설 리스트")
    private List<SearchMainFacilityRes> mainFacilityList;

    public SearchBuildingDetailRes(List<SearchMainFacilityRes> facilities, List<PlaceType> types, Building building, String imageUrl, boolean bookmarked) {
        this.buildingId = building.getId();
        this.name = "고려대학교 서울캠퍼스 " + building.getName();
        this.address = building.getAddress();
        this.latitude = building.getNode().getLatitude();
        this.longitude = building.getNode().getLongitude();
        this.imageUrl = imageUrl != null ? imageUrl : building.getImageUrl();
        this.weekdayOperatingTime = building.getWeekdayOperatingTime();
        this.saturdayOperatingTime = building.getSaturdayOperatingTime();
        this.sundayOperatingTime = building.getSundayOperatingTime();
        this.details = building.getDetail();
        this.bookmarked = bookmarked;
        this.existTypes = types;
        this.mainFacilityList = facilities;
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
