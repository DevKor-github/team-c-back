package devkor.com.teamcback.domain.search.dto.request;

import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
public class SearchPlaceReq {
    private PlaceType placeType;
    private Long id;
}
