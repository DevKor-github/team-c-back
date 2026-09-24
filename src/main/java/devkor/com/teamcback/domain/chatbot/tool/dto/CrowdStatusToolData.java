package devkor.com.teamcback.domain.chatbot.tool.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CrowdStatusToolData(Long placeId, Integer estimatedPeople, Integer capacity,
                                  CrowdLevel level, LocalDateTime measuredAt, boolean stale,
                                  List<TypicalCrowdPattern> typicalPattern) {
}
