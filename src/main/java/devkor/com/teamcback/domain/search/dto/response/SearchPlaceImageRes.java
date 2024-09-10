package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.PlaceImage;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchPlaceImageRes {
    private Long imageId;
    private String image;

    public SearchPlaceImageRes(PlaceImage placeImage) {
        this.imageId = placeImage.getId();
        this.image = placeImage.getImage();
    }
}
