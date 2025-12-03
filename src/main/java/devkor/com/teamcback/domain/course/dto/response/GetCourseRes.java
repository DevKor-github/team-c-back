package devkor.com.teamcback.domain.course.dto.response;

import devkor.com.teamcback.domain.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "강의 응답 dto")
@Getter
public class GetCourseRes {
    private Long courseId;
    private int year;
    private String term;
    private String subject;
    private int unit;
    private String code;
    private String section;
    private String type;
    private String department;
    private String professor;
    private String weekday;
    private int classTime;

    public GetCourseRes(Course course, String weekday, int classTime) {
        this.courseId = course.getId();
        this.year = course.getYear();
        this.term = course.getTerm().toString();
        this.subject = course.getSubject();
        this.unit = course.getUnit();
        this.code = course.getCode();
        this.section = course.getSection();
        this.type = course.getType();
        this.department = course.getDepartment();
        this.professor = course.getProfessor();
        this.weekday = weekday;
        this.classTime = classTime;
    }
}
