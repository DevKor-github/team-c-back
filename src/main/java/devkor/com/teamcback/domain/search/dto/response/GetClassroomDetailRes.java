package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import lombok.Getter;

@Getter
public class GetClassroomDetailRes {
    private Long classroomId;
    private String name;
    private String detail;
    private boolean plugAvailability;
    private String imageUrl;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;

    public GetClassroomDetailRes(Classroom classroom) {
        this.classroomId = classroom.getId();
        this.name = classroom.getName();
        this.detail = classroom.getDetail();
        this.plugAvailability = classroom.isPlugAvailability();
        this.imageUrl = classroom.getImageUrl();
        this.longitude = classroom.getLongitude();
        this.latitude = classroom.getLatitude();
        this.xCoord = classroom.getNode().getXCoord();
        this.yCoord = classroom.getNode().getYCoord();
    }
}
