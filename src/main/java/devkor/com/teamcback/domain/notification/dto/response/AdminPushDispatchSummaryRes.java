package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import java.time.LocalDateTime;

public record AdminPushDispatchSummaryRes(
        Long dispatchId,
        NotificationType notificationType,
        PushMode mode,
        AppVariant appVariant,
        PushTargetType targetType,
        String targetValue,
        String title,
        String body,
        PushActionType actionType,
        int recipientCount,
        PushDispatchStatus status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public AdminPushDispatchSummaryRes(PushDispatch dispatch) {
        this(
                dispatch.getPushDispatchId(),
                dispatch.getNotificationType(),
                dispatch.getMode(),
                dispatch.getAppVariant(),
                dispatch.getTargetType(),
                dispatch.getTargetValue(),
                dispatch.getTitle(),
                dispatch.getBody(),
                dispatch.getActionType(),
                dispatch.getRecipientCount(),
                dispatch.getStatus(),
                dispatch.getCreatedBy(),
                dispatch.getCreatedAt(),
                dispatch.getCompletedAt()
        );
    }
}
