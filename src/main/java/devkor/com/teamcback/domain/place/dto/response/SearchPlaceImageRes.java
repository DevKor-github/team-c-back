package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "장소 사진 검색 응답 dto")
@Getter
public class SearchPlaceImageRes {
    private String image;
    private Long sortNum;

    public SearchPlaceImageRes(Place place) {
        this.image = place.getImageUrl();
        this.sortNum = 1L;
    }

    public SearchPlaceImageRes(String image, Long sortNum) {
        this.image = image;
        this.sortNum = sortNum;
    }
}
