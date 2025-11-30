package devkor.com.teamcback.domain.schoolcalendar.dto.request;

import devkor.com.teamcback.domain.course.entity.Term;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "학기 업데이트")
@Getter
public class UpdateSchoolCalendarTermReq {
    private Term term;

    public UpdateSchoolCalendarTermReq(Term term) {
        this.term = term;
    }
}
