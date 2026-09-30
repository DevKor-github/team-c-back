package devkor.com.teamcback.domain.chatbot.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.place.entity.PlaceType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FacilityToolItem(
        Long placeId,
        String name,
        PlaceType placeType,
        Long buildingId,
        String buildingName,
        Integer floor,
        String detail,
        Boolean openNow,
        Boolean available,
        Boolean plugAvailable
) {
}
