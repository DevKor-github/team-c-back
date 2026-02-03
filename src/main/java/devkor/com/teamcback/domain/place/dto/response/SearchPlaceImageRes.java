package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "장소 사진 검색 응답 dto")
@Getter
public class SearchPlaceImageRes {
    private Long placeId;
    private String placeName;
    private String image;

    public SearchPlaceImageRes(Place place) {
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.image = place.getImageUrl();
    }

    public SearchPlaceImageRes(Place place, String image) {
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.image = image;
    }
}
