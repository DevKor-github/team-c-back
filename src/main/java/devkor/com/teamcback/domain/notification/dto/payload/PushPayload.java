package devkor.com.teamcback.domain.notification.dto.payload;

import java.util.Map;

public record PushPayload(
        String title,
        String body,
        PushPayloadData data
) {

    public record PushPayloadData(
            int version,
            String notificationId,
            PushPayloadAction action
    ) {
    }

    public record PushPayloadAction(
            String type,
            Map<String, Object> params
    ) {
    }
}
