package devkor.com.teamcback.domain.ble.dto.response;

import lombok.Getter;

@Getter
public class BLETimePatternRes {
    private Long placeId;     // 요청 placeId
    private int[] hours;      // {7,10,13,16,19,21,24}
    private String[] dayOfWeeks; // {1,2,3,4,5,6,7} (java.time.DayOfWeek 값)
    private int[][] averages; // [dayIndex][timeIndex] 형태, 각 원소는 반올림된 int

    public BLETimePatternRes(Long placeId, int[] timeSlots, String[] dayOfWeeks, int[][] averages) {
        this.placeId = placeId;
        this.hours = timeSlots;
        this.dayOfWeeks = dayOfWeeks;
        this.averages = averages;
    }
}
