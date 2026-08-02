package devkor.com.teamcback.domain.notification.dto.payload;

import java.util.Map;

public record PushPayload(
        String title,
        String body,
        PushPayloadData data
) {

    public record PushPayloadData(
            int schemaVersion,
            String actionType,
            Map<String, Object> actionParams
    ) {
    }
}
