package devkor.com.teamcback.domain.notification.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.validation.PushActionValidator;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

@Component
@RequiredArgsConstructor
public class PushPayloadFactory {

    private static final int PAYLOAD_VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES = 4096;
    private static final int MAX_IMAGE_URL_LENGTH = 1024;
    private static final String SIZE_VALIDATION_NOTIFICATION_ID = "00000000-0000-4000-8000-000000000000";

    private final PushActionValidator actionValidator;
    private final ObjectMapper objectMapper;

    public PushPayload create(
            String notificationId,
            String title,
            String body,
            PushMode mode,
            AppVariant appVariant,
            PushActionType actionType,
            Map<String, Object> actionParams
    ) {
        return create(
                notificationId,
                title,
                body,
                mode,
                appVariant,
                actionType,
                actionParams,
                null
        );
    }

    public PushPayload create(
            String notificationId,
            String title,
            String body,
            PushMode mode,
            AppVariant appVariant,
            PushActionType actionType,
            Map<String, Object> actionParams,
            String imageUrl
    ) {
        validateText(notificationId);
        validateText(title);
        validateText(body);
        String normalizedImageUrl = normalizeImageUrl(imageUrl);

        Map<String, Object> normalizedActionParams = actionValidator.validateAndNormalize(
                actionType,
                mode,
                appVariant,
                actionParams
        );

        PushPayload payload = new PushPayload(
                title,
                body,
                normalizedImageUrl,
                new PushPayload.PushPayloadData(
                        PAYLOAD_VERSION,
                        notificationId,
                        appVariant.toValue(),
                        new PushPayload.PushPayloadAction(
                                actionType.name(),
                                normalizedActionParams
                        )
                )
        );

        validatePayloadSize(payload);
        return payload;
    }

    public PushPayload createForPreDispatchValidation(
            String title,
            String body,
            PushMode mode,
            AppVariant appVariant,
            PushActionType actionType,
            Map<String, Object> actionParams
    ) {
        return createForPreDispatchValidation(
                title,
                body,
                mode,
                appVariant,
                actionType,
                actionParams,
                null
        );
    }

    public PushPayload createForPreDispatchValidation(
            String title,
            String body,
            PushMode mode,
            AppVariant appVariant,
            PushActionType actionType,
            Map<String, Object> actionParams,
            String imageUrl
    ) {
        // PushMessage uses an identity Long, so the worker must create the final payload with the real message id and re-check size.
        return create(
                SIZE_VALIDATION_NOTIFICATION_ID,
                title,
                body,
                mode,
                appVariant,
                actionType,
                actionParams,
                imageUrl
        );
    }

    public String serializeActionParams(Map<String, Object> actionParams) {
        try {
            return objectMapper.writeValueAsString(actionParams);
        } catch (JsonProcessingException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    public Map<String, Object> deserializeActionParams(String actionParams) {
        if (actionParams == null || actionParams.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(actionParams, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private void validateText(String value) {
        if (value == null || value.isBlank()) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String normalized = imageUrl.trim();
        if (normalized.length() > MAX_IMAGE_URL_LENGTH || !normalized.startsWith("https://")) {
            throw new GlobalException(INVALID_INPUT);
        }

        try {
            new URI(normalized);
        } catch (URISyntaxException e) {
            throw new GlobalException(INVALID_INPUT);
        }

        return normalized;
    }

    private void validatePayloadSize(PushPayload payload) {
        try {
            if (objectMapper.writeValueAsBytes(payload).length > MAX_PAYLOAD_BYTES) {
                throw new GlobalException(INVALID_INPUT);
            }
        } catch (JsonProcessingException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }
}
