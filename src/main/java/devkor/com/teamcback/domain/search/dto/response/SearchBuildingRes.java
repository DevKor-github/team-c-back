package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchBuildingRes {
    List<GetBuildingDetailRes> buildingList;

    public SearchBuildingRes(List<GetBuildingDetailRes> buildingList) {
        this.buildingList = buildingList;
    }
}
