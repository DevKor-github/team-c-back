package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import lombok.Getter;

@Getter
public class GetClassroomNicknameRes {
    private Long nicknameId;
    private String nickname;

    public GetClassroomNicknameRes(PlaceNickname placeNickname) {
        nicknameId = placeNickname.getId();
        nickname = placeNickname.getNickname();
    }
}
