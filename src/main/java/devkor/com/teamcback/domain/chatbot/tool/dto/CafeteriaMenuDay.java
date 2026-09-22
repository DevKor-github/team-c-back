package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.time.LocalDate;
import java.util.List;

public record CafeteriaMenuDay(LocalDate date, List<CafeteriaMealToolItem> meals) {
}
