package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "장소 대응 maskIndex 정보")
public class SearchMaskIndexByPlaceRes {
    @Schema(description = "Mask Index", example = "1")
    private Integer maskIndex;
    @Schema(description = "건물 Id", example = "1")
    private Long buildingId;
    @Schema(description = "건물 층", example = "1")
    private int floor;

    public SearchMaskIndexByPlaceRes(Place place) {
        this.maskIndex = place.getMaskIndex();
        this.buildingId = place.getBuilding().getId();
        this.floor = place.getFloor().intValue();
    }
}
