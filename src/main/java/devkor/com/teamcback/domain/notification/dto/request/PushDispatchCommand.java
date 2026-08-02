package devkor.com.teamcback.domain.notification.dto.request;

import devkor.com.teamcback.domain.notification.entity.AppVariant;
import devkor.com.teamcback.domain.notification.entity.NotificationType;
import devkor.com.teamcback.domain.notification.entity.PushActionType;
import devkor.com.teamcback.domain.notification.entity.PushMode;
import devkor.com.teamcback.domain.notification.entity.PushTargetType;
import java.util.Map;

public record PushDispatchCommand(
        NotificationType notificationType,
        PushMode mode,
        AppVariant appVariant,
        PushTargetType targetType,
        String targetValue,
        String title,
        String body,
        PushActionType actionType,
        Map<String, Object> actionParams,
        String idempotencyKey,
        Long createdBy
) {
}
