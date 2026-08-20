package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.type.PushEventType;

public record AdminPushEventFlagRes(
        PushEventType eventType,
        boolean enabled
) {
}
