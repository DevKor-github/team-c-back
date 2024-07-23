package devkor.com.teamcback.domain.admin.classroom.dto.response;

import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import lombok.Getter;

@Getter
public class GetClassroomNicknameRes {
    private Long nicknameId;
    private String nickname;

    public GetClassroomNicknameRes(ClassroomNickname classroomNickname) {
        nicknameId = classroomNickname.getId();
        nickname = classroomNickname.getNickname();
    }
}
