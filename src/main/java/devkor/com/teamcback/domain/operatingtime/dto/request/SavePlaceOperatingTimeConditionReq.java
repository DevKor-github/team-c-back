package devkor.com.teamcback.domain.operatingtime.dto.request;

import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "저장할 운영시간 정보(상관없는 조건은 null로 표시)")
@Getter
@Setter
public class SavePlaceOperatingTimeConditionReq {
    @Schema(description = "요일", example = "WEEKDAY")
    private DayOfWeek dayOfWeek;
    @Schema(description = "짝수주 여부", example = "null")
    private Boolean isEvenWeek;
    @Schema(description = "공휴일 여부", example = "false")
    private Boolean isHoliday;
    @Schema(description = "방학 여부", example = "null")
    private Boolean isVacation;
    @Schema(description = "조건에 해당하는 운영시간 목록")
    @NotEmpty(message = "최소 1개의 운영시간이 있어야 합니다.")
    private List<SavePlaceOperatingTimeConditionTimeReq> timeList;
}
