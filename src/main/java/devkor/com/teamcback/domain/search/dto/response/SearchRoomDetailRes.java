package devkor.com.teamcback.domain.search.dto.response;

import static devkor.com.teamcback.domain.operatingtime.service.OperatingService.OPERATING_TIME_PATTERN;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class SearchRoomDetailRes {
    private Long id;
    @JsonInclude(Include.NON_NULL)
    private PlaceType placeType;
    private String name;
    private String detail;
    @JsonInclude(Include.NON_NULL)
    private boolean availability;
    private boolean plugAvailability;
    private String imageUrl;
    private String weekdayOperatingTime;
    private String saturdayOperatingTime;
    private String sundayOperatingTime;
    private boolean isOperating;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;
    private Integer maskIndex;
    private String description;
    private String starAverage;
    private String nextPlaceTime;

    public SearchRoomDetailRes(Place place, String imageUrl) {
        this.id = place.getId();
        this.placeType = place.getType();
        this.name = place.getName();
        this.detail = place.getDetail();
        this.availability = place.isAvailability();
        this.plugAvailability = place.isPlugAvailability();
        this.imageUrl = imageUrl != null ? imageUrl : place.getImageUrl();
        this.weekdayOperatingTime = place.getWeekdayOperatingTime();
        this.saturdayOperatingTime = place.getSaturdayOperatingTime();
        this.sundayOperatingTime = place.getSundayOperatingTime();
        this.isOperating = place.isOperating();
        this.longitude = place.getNode().getLongitude();
        this.latitude = place.getNode().getLatitude();
        this.xCoord = place.getNode().getXCoord();
        this.yCoord = place.getNode().getYCoord();
        this.maskIndex = place.getMaskIndex();
        this.description = place.getDescription();
        this.starAverage = String.format("%.2f", ((double) place.getStarSum()) / place.getStarNum());
        if(place.getOperatingTime() == null || !Pattern.matches(OPERATING_TIME_PATTERN, place.getOperatingTime())) {
            this.nextPlaceTime = null;
        }
        else if(place.isOperating()) { // 운영 중이면 종료 시간
            this.nextPlaceTime = place.getOperatingTime().substring(6, 11);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextPlaceTime = place.getOperatingTime().substring(0, 5);
        }
    }
}
