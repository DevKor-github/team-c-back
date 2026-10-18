package devkor.com.teamcback.domain.chatbot.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetLocationDetailToolResult(LocationDetailToolData location, CampusToolError error) {
}
