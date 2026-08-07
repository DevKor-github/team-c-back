package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import java.time.LocalDateTime;
import java.util.Map;

public record AdminPushDispatchDetailRes(
        Long dispatchId,
        NotificationType notificationType,
        PushMode mode,
        AppVariant appVariant,
        PushTargetType targetType,
        String targetValue,
        String title,
        String body,
        String imageUrl,
        PushActionType actionType,
        String actionParams,
        int recipientCount,
        PushDispatchStatus status,
        Map<PushMessageStatus, Long> messageStatusCounts,
        String idempotencyKey,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public AdminPushDispatchDetailRes(
            PushDispatch dispatch,
            Map<PushMessageStatus, Long> messageStatusCounts
    ) {
        this(
                dispatch.getPushDispatchId(),
                dispatch.getNotificationType(),
                dispatch.getMode(),
                dispatch.getAppVariant(),
                dispatch.getTargetType(),
                dispatch.getTargetValue(),
                dispatch.getTitle(),
                dispatch.getBody(),
                dispatch.getImageUrl(),
                dispatch.getActionType(),
                dispatch.getActionParams(),
                dispatch.getRecipientCount(),
                dispatch.getStatus(),
                messageStatusCounts,
                dispatch.getIdempotencyKey(),
                dispatch.getCreatedBy(),
                dispatch.getCreatedAt(),
                dispatch.getCompletedAt()
        );
    }
}
