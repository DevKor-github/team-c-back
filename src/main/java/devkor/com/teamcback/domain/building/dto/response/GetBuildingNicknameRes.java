package devkor.com.teamcback.domain.building.dto.response;

import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import lombok.Getter;

@Getter
public class GetBuildingNicknameRes {
    private Long nicknameId;
    private String nickname;

    public GetBuildingNicknameRes(BuildingNickname buildingNickname) {
        nicknameId = buildingNickname.getId();
        nickname = buildingNickname.getNickname();
    }
}
