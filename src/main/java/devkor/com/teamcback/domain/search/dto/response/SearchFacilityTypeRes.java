package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.facility.entity.FacilityType;
import java.util.List;
import lombok.Getter;

@Getter
public class SearchFacilityTypeRes {
    private List<FacilityType> typeList;

    public SearchFacilityTypeRes(List<FacilityType> facilityTypeList) {
        this.typeList = facilityTypeList;
    }
}
