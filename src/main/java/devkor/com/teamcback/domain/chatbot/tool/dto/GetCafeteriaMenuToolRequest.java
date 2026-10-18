package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.time.LocalDate;

public record GetCafeteriaMenuToolRequest(Long placeId, LocalDate startDate, LocalDate endDate) {
}
