package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "건물에 있는 편의시설 종류")
public class SearchFacilityTypeRes {
    private List<PlaceType> typeList;

    public SearchFacilityTypeRes(List<PlaceType> placeTypeList) {
        this.typeList = placeTypeList;
    }
}
