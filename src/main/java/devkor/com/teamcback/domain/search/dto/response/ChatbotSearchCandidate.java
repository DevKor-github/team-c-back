package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;

/** Minimal, node-free search result for chatbot candidate resolution. */
public record ChatbotSearchCandidate(
        Long locationId,
        LocationType locationType,
        String name,
        Long buildingId,
        Double floor,
        PlaceType placeType,
        String detail
) {
}
