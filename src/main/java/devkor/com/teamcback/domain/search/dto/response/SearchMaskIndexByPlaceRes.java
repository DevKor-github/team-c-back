package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
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

    public SearchMaskIndexByPlaceRes(Classroom classroom) {
        this.maskIndex = classroom.getMaskIndex();
        this.buildingId = classroom.getBuilding().getId();
        this.floor = classroom.getFloor().intValue();
    }
    public SearchMaskIndexByPlaceRes(Facility facility) {
        this.maskIndex = facility.getMaskIndex();
        this.buildingId = facility.getBuilding().getId();
        this.floor = facility.getFloor().intValue();
    }
}
