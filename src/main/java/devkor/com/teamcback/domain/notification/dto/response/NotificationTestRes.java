package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;

public record NotificationTestRes(
        String notificationId,
        String installationId,
        AppVariant appVariant,
        PushMessageStatus messageStatus,
        String ticketId
) {
}
