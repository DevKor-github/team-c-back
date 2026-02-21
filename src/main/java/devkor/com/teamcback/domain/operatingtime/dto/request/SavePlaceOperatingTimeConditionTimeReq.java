package devkor.com.teamcback.domain.operatingtime.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavePlaceOperatingTimeConditionTimeReq {
    @Min(value = 0)
    @Max(value = 23)
    private int StartHour;
    @Min(value = 0)
    @Max(value = 59)
    private int StartMinute;
    @Min(value = 0)
    @Max(value = 23)
    private int endHour;
    @Min(value = 0)
    @Max(value = 59)
    private int endMinute;
}
