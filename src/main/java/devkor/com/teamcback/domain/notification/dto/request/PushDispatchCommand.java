package devkor.com.teamcback.domain.notification.dto.request;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
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
