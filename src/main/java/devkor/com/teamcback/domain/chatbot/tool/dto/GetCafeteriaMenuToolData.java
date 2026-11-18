package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.util.List;

public record GetCafeteriaMenuToolData(Long placeId, String placeName, List<CafeteriaMenuDay> days) {
}
