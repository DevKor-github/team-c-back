package devkor.com.teamcback.domain.search.dto.request;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "검색결과 id, 이름, type")
@Getter
public class SaveSearchLogReq {
    @Schema(description = "검색 시설 id", example = "5")
    private Long id;
    @Schema(description = "검색 시설 이름", example = "우정정보관 201호")
    private String name;
    @Schema(description = "건물 또는 시설 종류", example = "BUILDING")
    private LocationType locationType;
    @Schema(description = "강의실 층수", example = "3")
    private Double floor;
    @Schema(description = "건물 주소", example = "서울특별시 성북구 안암로 145 고려대학교 애기능생활관")
    private String address;
    @Schema(description = "설명", example = "프리액션 밸브실")
    private String detail;
    @Schema(description = "건물 또는 시설 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "건물 또는 시설 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "편의시설 종류", example = "LOUNGE")
    private PlaceType placeType;
    @Schema(description = "building_id", example = "null/0/1")
    private Long buildingId;
}
