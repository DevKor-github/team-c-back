package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Schema(description = "편의시설 조회 결과")
@Getter
public class SearchFacilityListRes {
    @Schema(description = "편의시설 조회 결과")
    private List<SearchPlaceRes> facilities = new ArrayList<>();

    public SearchFacilityListRes(List<SearchPlaceRes> facilities) {
        this.facilities = facilities;
    }
}
