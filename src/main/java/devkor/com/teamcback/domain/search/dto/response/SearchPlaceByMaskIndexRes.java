package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "maskIndex 대응 장소 정보")
public class SearchPlaceByMaskIndexRes {
    @Schema(description = "장소 Id", example = "1")
    private Long placeId;

    public SearchPlaceByMaskIndexRes(Place place) {
        this.placeId = place.getId();
    }
}
