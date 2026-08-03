package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;

public record NotificationTestRes(
        String notificationId,
        String installationId,
        AppVariant appVariant,
        String ticketStatus,
        String ticketId
) {
}
