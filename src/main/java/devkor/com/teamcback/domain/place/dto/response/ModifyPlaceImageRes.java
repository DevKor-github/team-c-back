package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "장소 사진 수정 완료")
@Getter
public class ModifyPlaceImageRes {
    private Long placeId;
    private String imageUrl;

    public ModifyPlaceImageRes(Place place, String imageUrl) {
        this.placeId = place.getId();
        this.imageUrl = imageUrl;
    }
}
