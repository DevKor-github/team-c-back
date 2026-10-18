package devkor.com.teamcback.domain.chatbot.tool.dto;

public record CampusStatusToolResult(String term, boolean vacation, boolean koyeonPeriod,
                                     CampusToolError error) {
}
