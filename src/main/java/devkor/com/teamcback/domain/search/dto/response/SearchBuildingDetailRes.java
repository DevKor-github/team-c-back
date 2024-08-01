package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "건물 상세 정보")
public class SearchBuildingDetailRes {
    @Schema(description = "건물 id", example = "1")
    private Long buildingId;
    @Schema(description = "건물명", example = "애기능생활관")
    private String name;
    @Schema(description = "주소", example = "서울 성북구 안암로 73-15")
    private String address;
    @Schema(description = "건물 사진 url", example = "buildingurlurl")
    private String imageUrl;
    @Schema(description = "운영 시간", example = "9:00-22:00")
    private String operatingTime;
    @Schema(description = "건물 정보(TMI)", example = "애기능생활관이다.")
    private String details;
    @Schema(description = "북마크 저장 여부", example = "false")
    private boolean bookmarked;
    @Schema(description = "건물 내 시설 종류", example = "LOUNGE, GYM, ...")
    private List<FacilityType> existTypes; //건물 내 facility 종류 리스트(아이콘용)
    @Schema(description = "주요 시설 리스트")
    private List<GetMainFacilityRes> mainFacilityList;
    @Schema(description = "운영 여부", example = "false")
    private boolean isOperating;
    @Schema(description = "현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환", example = "9:00")
    private String nextBuildingTime; // 현재 열려있으면 닫는 시간, 닫혀있으면 다음으로 여는 시간 반환

    public SearchBuildingDetailRes(List<GetMainFacilityRes> facilities, List<FacilityType> types, Building building, boolean bookmarked) {
        this.buildingId = building.getId();
        this.name = "고려대학교 서울캠퍼스 " + building.getName();
        this.address = building.getAddress();
        this.imageUrl = building.getImageUrl();
        this.operatingTime = building.getOperatingTime();
        this.details = building.getDetail();
        this.bookmarked = bookmarked;
        this.existTypes = types;
        this.mainFacilityList = facilities;
        //TODO: 차후 isOperating, nextOperatingTime 수정하기(현재 임시값)
        this.isOperating = false;
        this.nextBuildingTime = "9:00";
    }
}
