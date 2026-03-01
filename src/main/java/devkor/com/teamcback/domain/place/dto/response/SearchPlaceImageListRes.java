package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "장소 사진 리스트 응답 dto")
@Getter
public class SearchPlaceImageListRes {
    private Long placeId;
    private String placeName;
    private List<SearchPlaceImageRes> imageList;

    public SearchPlaceImageListRes(Place place, List<SearchPlaceImageRes> imageList) {
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.imageList = imageList;
    }
}
