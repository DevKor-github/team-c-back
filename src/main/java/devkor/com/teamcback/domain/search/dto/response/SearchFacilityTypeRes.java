package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.PlaceType;
import java.util.List;
import lombok.Getter;

@Getter
public class SearchFacilityTypeRes {
    private List<PlaceType> typeList;

    public SearchFacilityTypeRes(List<PlaceType> placeTypeList) {
        this.typeList = placeTypeList;
    }
}
