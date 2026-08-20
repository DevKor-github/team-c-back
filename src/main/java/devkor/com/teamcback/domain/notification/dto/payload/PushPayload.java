package devkor.com.teamcback.domain.notification.dto.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record PushPayload(
        String title,
        String body,
        @JsonInclude(JsonInclude.Include.NON_NULL) String image,
        PushPayloadData data
) {

    public PushPayload(
            String title,
            String body,
            PushPayloadData data
    ) {
        this(title, body, null, data);
    }

    public record PushPayloadData(
            int version,
            String notificationId,
            String appVariant,
            PushPayloadAction action
    ) {
    }

    public record PushPayloadAction(
            String type,
            Map<String, Object> params
    ) {
    }
}
