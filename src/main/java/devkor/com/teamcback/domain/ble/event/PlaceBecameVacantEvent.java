package devkor.com.teamcback.domain.ble.event;

import java.time.LocalDateTime;

public record PlaceBecameVacantEvent(
        Long placeId,
        Long bleDataId,
        LocalDateTime occurredAt
) {
}
