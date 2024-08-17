package devkor.com.teamcback.domain.classroom.dto.response;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "교실 응답 dto")
@Getter
public class GetClassroomRes {
    private Long classroomId;
    private Long nodeId;
    private String detail;
    private String name;
    private Integer maskIndex;
    private boolean isPlugAvailability;
    private boolean isOperating;
    private String operatingTime;
    private String imageUrl;

    public GetClassroomRes(Classroom classroom) {
        this.classroomId = classroom.getId();
        this.nodeId = classroom.getNode().getId();
        this.detail = classroom.getDetail();
        this.name = classroom.getName();
        this.maskIndex = classroom.getMaskIndex();
        this.isPlugAvailability = classroom.isPlugAvailability();
        this.isOperating = classroom.isOperating();
        this.operatingTime = classroom.getOperatingTime();
        this.imageUrl = classroom.getImageUrl();
    }



}
