package devkor.com.teamcback.domain.admin.classroom.dto.response;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "교실 생성 응답 dto")
@Getter
public class CreateClassroomRes {
    private Long classroomId;

    public CreateClassroomRes(Classroom classroom) {
        this.classroomId = classroom.getId();
    }
}
