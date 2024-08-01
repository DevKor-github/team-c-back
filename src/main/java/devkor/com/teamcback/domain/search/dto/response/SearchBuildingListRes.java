package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "건물 목록")
public class SearchBuildingListRes {
    List<SearchBuildingRes> list;

    public SearchBuildingListRes(List<SearchBuildingRes> buildingList) {
        this.list = buildingList;
    }
}
