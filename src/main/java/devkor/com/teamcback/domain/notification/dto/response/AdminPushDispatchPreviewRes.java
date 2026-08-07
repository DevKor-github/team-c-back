package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import java.util.List;

public record AdminPushDispatchPreviewRes(
        int recipientCount,
        List<AdminPushInstallationRes> recipients,
        PushPayload payload
) {
}
