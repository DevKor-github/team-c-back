package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "특정 건물의 편의시설 조회 결과")
@Getter
public class SearchBuildingFacilityListRes {
    @Schema(description = "편의시설이 위치한 건물 ID", example = "1")
    private Long buildingId;
    @Schema(description = "편의시설이 위치한 건물 이름", example = "애기능생활관")
    private String buildingName;
    @Schema(description = "각 층별 편의시설 조회 결과")
    @Setter
    private Map<Double, List<SearchFacilityRes>> facilities = new HashMap<>();

    public SearchBuildingFacilityListRes(Building building) {
        this.buildingId = building.getId();
        this.buildingName = building.getName();
    }
}
