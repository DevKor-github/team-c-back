package devkor.com.teamcback.domain.classroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "강의실 List 조회 응답 dto")
@Getter
public class GetClassroomListRes {
    private List<GetClassroomRes> classroomList;

    public GetClassroomListRes(List<GetClassroomRes> classroomList) {
        this.classroomList = classroomList;
    }
}
