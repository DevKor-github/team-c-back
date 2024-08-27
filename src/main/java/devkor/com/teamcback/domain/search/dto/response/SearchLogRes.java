package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "검색 기록 정보")
@Getter
public class SearchLogRes {
    @Schema(description = "검색 시설 id", example = "5")
    private Long id;
    @Schema(description = "검색 시설 이름", example = "우정정보관 201호")
    private String name;
    @Schema(description = "검색 시설 종류", example = "CLASSROOM")
    private LocationType type;
    @Schema(description = "검색 일시", example = "2024-05-09")
    private String searchedAt;

    public SearchLogRes(SearchLog searchLog) {
        this.id = searchLog.getId();
        this.name = searchLog.getName();
        this.type = searchLog.getType();
        this.searchedAt = searchLog.getSearchedAt();
    }
}
