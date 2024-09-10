package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "장소 응답 dto")
@Getter
public class GetPlaceRes {
    private Long placeId;
    private Long nodeId;
    private String detail;
    private String name;
    private PlaceType type;
    private String operatingTime;
    private String imageUrl;
    private boolean availability;
    private boolean plugAvailability;
    private boolean isOperating;
    private Integer maskIndex;
    private String description;
    private double starAverage;

    public GetPlaceRes(Place place) {
        this.placeId = place.getId();
        this.nodeId = place.getNode().getId();
        this.detail = place.getDetail();
        this.name = place.getName();
        this.type = place.getType();
        this.operatingTime = place.getOperatingTime();
        this.imageUrl = place.getImageUrl();
        this.availability = place.isAvailability();
        this.plugAvailability = place.isPlugAvailability();
        this.isOperating = place.isOperating();
        this.maskIndex = place.getMaskIndex();
        this.description = place.getDescription();
        this.starAverage = ((double) place.getStarSum()) / place.getStarNum();
    }

}
