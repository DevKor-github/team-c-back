package devkor.com.teamcback.domain.admin.classroom.dto.response;

import devkor.com.teamcback.domain.admin.building.dto.response.GetBuildingNicknameRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Schema(description = "강의실 별명 조회 응답 dto")
@Getter
public class GetClassroomNicknameListRes {
    private Long classroomId;
    private String classroomName;
    private List<GetClassroomNicknameRes> nicknameList;

    public GetClassroomNicknameListRes(Classroom classroom, List<GetClassroomNicknameRes> nicknameList) {
        this.classroomId = classroom.getId();
        this.classroomName = classroom.getName();
        this.nicknameList = nicknameList;
    }
}
