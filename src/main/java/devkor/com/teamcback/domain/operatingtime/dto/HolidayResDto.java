package devkor.com.teamcback.domain.operatingtime.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.json.JSONObject;

@Getter
@NoArgsConstructor
public class HolidayResDto {
    private String name;
    private boolean isHoliday;
    private LocalDate date;

    public HolidayResDto(JSONObject itemJson) {
        this.name = itemJson.getString("dateName");
        String holidayOrNot = itemJson.getString("isHoliday");
        this.isHoliday = holidayOrNot.equals("Y") ? true : false;
        String dateString = itemJson.getBigInteger("locdate").toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        this.date = LocalDate.parse(dateString, formatter);
    }
}
