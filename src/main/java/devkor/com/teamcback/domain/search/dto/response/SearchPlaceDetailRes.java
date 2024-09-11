package devkor.com.teamcback.domain.search.dto.response;

import static devkor.com.teamcback.domain.operatingtime.service.OperatingService.OPERATING_TIME_PATTERN;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchPlaceDetailRes {
    private Long buildingId;
    private int floor;
    private LocationType type;
    private Long placeId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlaceType placeType;
    private String name;
    private String detail;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean availability;  //사용 가능 여부
    private boolean plugAvailability;
    private String imageUrl;
    private String weekdayOperatingTime;
    private String saturdayOperatingTime;
    private String sundayOperatingTime;
    private Double longitude;
    private Double latitude;
    private double xCoord;
    private double yCoord;
    private Integer maskIndex;
    private boolean bookmarked;
    private boolean isOperating;
    private String nextPlaceTime;
    private String description;
    private String starAverage;
    private List<SearchPlaceImageRes> placeImages;

    public SearchPlaceDetailRes(Place place, boolean bookmarked, List<SearchPlaceImageRes> placeImages) {
        this.buildingId = place.getBuilding().getId();
        this.floor = place.getFloor().intValue();
        this.type = LocationType.PLACE;
        this.placeId = place.getId();
        this.placeType = place.getType();
        if(place.getBuilding().getId() == 0) {
            this.name = place.getName();
            this.longitude = place.getNode().getLongitude();
            this.latitude = place.getNode().getLatitude();
        } else {
            this.name = place.getBuilding().getName() + " " + place.getName();
            this.xCoord = place.getNode().getXCoord();
            this.yCoord = place.getNode().getYCoord();
        }
        this.detail = place.getDetail();
        this.availability = place.isAvailability();
        this.plugAvailability = place.isPlugAvailability();
        this.imageUrl = place.getImageUrl();
        this.weekdayOperatingTime = place.getWeekdayOperatingTime();
        this.saturdayOperatingTime = place.getSaturdayOperatingTime();
        this.sundayOperatingTime = place.getSundayOperatingTime();
        this.maskIndex = place.getMaskIndex();
        this.bookmarked = bookmarked;
        this.isOperating = place.isOperating();
        if(place.getOperatingTime() == null || !Pattern.matches(OPERATING_TIME_PATTERN, place.getOperatingTime())) {
            this.nextPlaceTime = null;
        }
        else if(place.isOperating()) { // 운영 중이면 종료 시간
            this.nextPlaceTime = place.getOperatingTime().substring(6, 11);
        }
        else { // 운영 종료인 경우 여는 시간
            this.nextPlaceTime = place.getOperatingTime().substring(0, 5);
        }
        this.description = place.getDescription();
        this.starAverage = String.format("%.2f", ((double) place.getStarSum()) / place.getStarNum());
        this.placeImages = placeImages;
    }
}
