package devkor.com.teamcback.domain.chatbot.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindFacilitiesToolResult(List<FacilityToolItem> facilities, CampusToolError error) {
}
