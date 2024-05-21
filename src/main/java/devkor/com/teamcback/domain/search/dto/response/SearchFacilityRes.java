package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SearchFacilityRes {
    private Long buildingId;
    private String buildingName;
    private FacilityType type;
    @Setter
    private Map<Integer, List<GetFacilityRes>> facilities = new HashMap<>();

    public SearchFacilityRes(Building building, FacilityType type) {
        this.buildingId = building.getId();
        this.buildingName = building.getName();
        this.type = type;
    }
}
