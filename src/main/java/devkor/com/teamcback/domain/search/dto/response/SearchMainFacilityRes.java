package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class SearchMainFacilityRes {
    @Schema(description = "편의시설명", example = "2층 학생식당")
    private String name;
    @Schema(description = "편의시설 종류", example = "CAFETERIA")
    private PlaceType type;
    @Schema(description = "편의시설 종류(한글)", example = "식당")
    private String typeName;
    @Schema(description = "편의시설 id", example = "1")
    private Long placeId;
    @Schema(description = "이미지 URL")
    private String imageUrl;

    public SearchMainFacilityRes(Place place, String imageUrl) {
        if(place.getFloor() > 0) {
            int floor = (int) Math.floor(place.getFloor());
            this.name = floor + "층 " + place.getName();
        } else {
            int floor = (int) Math.floor(place.getFloor() * -1);
            this.name = "B" + floor + "층 " + place.getName();
        }
        this.type = place.getType();
        this.typeName = place.getType().getName();
        this.placeId = place.getId();
        this.imageUrl = imageUrl != null ? imageUrl : place.getImageUrl();
    }
}
