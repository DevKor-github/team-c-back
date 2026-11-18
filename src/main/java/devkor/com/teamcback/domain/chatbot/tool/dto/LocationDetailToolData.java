package devkor.com.teamcback.domain.chatbot.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.place.entity.PlaceType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationDetailToolData(
        Long locationId,
        ToolLocationType locationType,
        String name,
        Long buildingId,
        String buildingName,
        Integer floor,
        String address,
        String detail,
        PlaceType placeType,
        Boolean openNow,
        String nextStatusChangeTime,
        String weekdayHours,
        String saturdayHours,
        String sundayHours,
        Boolean available,
        Boolean plugAvailable,
        Boolean studentCardRequired,
        Double rating
) {
}
