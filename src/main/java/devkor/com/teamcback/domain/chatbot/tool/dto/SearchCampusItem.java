package devkor.com.teamcback.domain.chatbot.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.place.entity.PlaceType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchCampusItem(
        Long locationId,
        ToolLocationType locationType,
        String name,
        Long buildingId,
        String buildingName,
        Double floor,
        PlaceType placeType,
        String detail
) {
}
