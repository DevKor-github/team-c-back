package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.PlaceType;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class searchPlaceByMaskIndexRes {
    private Long placeId;
    private PlaceType placeType;

    public searchPlaceByMaskIndexRes(Classroom classroom) {
        this.placeType = PlaceType.CLASSROOM;
        this.placeId = classroom.getId();
    }
    public searchPlaceByMaskIndexRes(Facility facility) {
        this.placeType = PlaceType.FACILITY;
        this.placeId = facility.getId();
    }
}
