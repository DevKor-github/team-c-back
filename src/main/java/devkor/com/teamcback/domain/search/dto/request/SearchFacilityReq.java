package devkor.com.teamcback.domain.search.dto.request;

import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

@Getter
public class SearchFacilityReq {
    private Long buildingId;
    private FacilityType facilityType;
}
