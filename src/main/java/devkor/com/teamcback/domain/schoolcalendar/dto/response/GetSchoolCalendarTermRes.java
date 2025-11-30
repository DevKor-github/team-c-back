package devkor.com.teamcback.domain.schoolcalendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "학기 응답 dto")
@Getter
public class GetSchoolCalendarTermRes {

    @Schema(description = "학기", example = "SPRING")
    private String term;

    public GetSchoolCalendarTermRes(String term) {
        this.term = term;
    }

}
