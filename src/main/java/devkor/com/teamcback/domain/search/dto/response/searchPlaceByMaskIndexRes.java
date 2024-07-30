package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.PlaceType;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "maskIndex 대응 장소 정보")
public class searchPlaceByMaskIndexRes {
    @Schema(description = "장소 종류", example = "CLASSROOM")
    private PlaceType placeType;
    @Schema(description = "장소 Id", example = "1")
    private Long placeId;

    public searchPlaceByMaskIndexRes(Classroom classroom) {
        this.placeType = PlaceType.CLASSROOM;
        this.placeId = classroom.getId();
    }
    public searchPlaceByMaskIndexRes(Facility facility) {
        this.placeType = PlaceType.FACILITY;
        this.placeId = facility.getId();
    }
}
