package devkor.com.teamcback.domain.admin.building.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Schema(description = "건물 별명 조회 응답 dto")
@Getter
public class GetBuildingNicknameListRes {
    private Long buildingId;
    private String buildingName;
    private List<GetBuildingNicknameRes> nicknameList;

    public GetBuildingNicknameListRes(Building building, List<GetBuildingNicknameRes> nicknameList) {
        this.buildingId = building.getId();
        this.buildingName = building.getName();
        this.nicknameList = nicknameList;
    }
}
