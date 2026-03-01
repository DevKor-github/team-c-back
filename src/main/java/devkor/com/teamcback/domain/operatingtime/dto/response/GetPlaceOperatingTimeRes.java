package devkor.com.teamcback.domain.operatingtime.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import lombok.Getter;

@Getter
public class GetPlaceOperatingTimeRes {
    private Long placeId;
    private String placeName;
    private String todayOperatingTime;
    private String weekDayOperatingTime;
    private String mondayOperatingTime;
    private String tuesdayOperatingTime;
    private String wednesdayOperatingTime;
    private String thursdayOperatingTime;
    private String fridayOperatingTime;
    private String saturdayOperatingTime;
    private String sundayOperatingTime;


    public GetPlaceOperatingTimeRes(Place place) {
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.todayOperatingTime = place.getOperatingTime();
        this.weekDayOperatingTime = place.getWeekdayOperatingTime();
        this.mondayOperatingTime = place.getMondayOperatingTime();
        this.tuesdayOperatingTime = place.getTuesdayOperatingTime();
        this.wednesdayOperatingTime = place.getWednesdayOperatingTime();
        this.thursdayOperatingTime = place.getThursdayOperatingTime();
        this.fridayOperatingTime = place.getFridayOperatingTime();
        this.saturdayOperatingTime = place.getSaturdayOperatingTime();
        this.sundayOperatingTime = place.getSundayOperatingTime();
    }
}
