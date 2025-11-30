package devkor.com.teamcback.domain.course.dto.response;

import devkor.com.teamcback.domain.common.entity.Weekday;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Schema(description = "강의 List 조회 응답 dto")
@Getter
public class GetCourseListRes {
    private Long placeId;
    private String placeName;
    private Map<Weekday, List<GetCourseRes>> courses;

    public GetCourseListRes(Long placeId, String placeName, Map<Weekday, List<GetCourseRes>> courses) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.courses = courses;
    }
}
