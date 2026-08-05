package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;

public record AdminPushDispatchPreviewRes(
        int recipientCount,
        PushPayload payload
) {
}
