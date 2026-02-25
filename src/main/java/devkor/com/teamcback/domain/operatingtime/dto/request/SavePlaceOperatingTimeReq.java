package devkor.com.teamcback.domain.operatingtime.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "저장할 운영시간 정보")
@Getter
@Setter
public class SavePlaceOperatingTimeReq {
    @Schema(description = "평일 운영시간", example = "09:00-21:00")
    private String weekDayOperatingTime;
    @Schema(description = "월요일 운영시간(평일 모두 같다면 없어도 됨)", example = "09:00-21:00")
    private String mondayOperatingTime;
    @Schema(description = "화요일 운영시간(평일 모두 같다면 없어도 됨)", example = "09:00-21:00")
    private String tuesdayOperatingTime;
    @Schema(description = "수요일 운영시간(평일 모두 같다면 없어도 됨)", example = "09:00-21:00")
    private String wednesdayOperatingTime;
    @Schema(description = "목요일 운영시간(평일 모두 같다면 없어도 됨)", example = "09:00-21:00")
    private String thursdayOperatingTime;
    @Schema(description = "금요일 운영시간(평일 모두 같다면 없어도 됨)", example = "09:00-21:00")
    private String fridayOperatingTime;
    @Schema(description = "토요일 운영시간", example = "09:00-20:00")
    private String saturdayOperatingTime;
    @Schema(description = "일요일 운영시간", example = "휴무")
    private String sundayOperatingTime;
}
