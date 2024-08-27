package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "편의시설 생성 응답 dto")
@Getter
public class CreatePlaceRes {
    private Long facilityID;

    public CreatePlaceRes(Place place) {
        this.facilityID = place.getId();
    }
}
