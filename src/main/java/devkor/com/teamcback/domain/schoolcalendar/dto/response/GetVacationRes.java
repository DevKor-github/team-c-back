package devkor.com.teamcback.domain.schoolcalendar.dto.response;

import devkor.com.teamcback.domain.schoolcalendar.entity.SchoolCalendar;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "방학 여부 응답 dto")
@Getter
public class GetVacationRes {

    @Schema(description = "학교 일정 ID", example = "1")
    private Long id;
    @Schema(description = "학교 일정 이름", example = "방학")
    private String name;
    @Schema(description = "일정 여부", example = "true")
    private boolean isActive;

    public GetVacationRes(SchoolCalendar schoolCalendar) {
        this.id = schoolCalendar.getId();
        this.name = schoolCalendar.getName();
        this.isActive = schoolCalendar.isActive();
    }
}
