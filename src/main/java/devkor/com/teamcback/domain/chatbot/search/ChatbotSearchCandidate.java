package devkor.com.teamcback.domain.chatbot.search;

import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;

/** Internal candidate; it is converted to the existing Tool DTO by the adapter. */
public record ChatbotSearchCandidate(Long locationId, ToolLocationType locationType, String name,
                                     Long buildingId, String buildingName, Double floor,
                                     PlaceType placeType, String detail, int sourcePriority) {
}
