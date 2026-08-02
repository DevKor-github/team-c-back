package devkor.com.teamcback.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.entity.AppVariant;
import devkor.com.teamcback.domain.notification.entity.PushActionType;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

@Component
@RequiredArgsConstructor
public class PushPayloadFactory {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES = 4096;

    private final PushActionValidator actionValidator;
    private final ObjectMapper objectMapper;

    public PushPayload create(
            String title,
            String body,
            PushActionType actionType,
            Map<String, Object> actionParams,
            AppVariant appVariant
    ) {
        validateText(title);
        validateText(body);

        Map<String, Object> normalizedActionParams = actionValidator.validateAndNormalize(
                actionType,
                appVariant,
                actionParams
        );

        PushPayload payload = new PushPayload(
                title,
                body,
                new PushPayload.PushPayloadData(
                        SCHEMA_VERSION,
                        actionType.name(),
                        normalizedActionParams
                )
        );

        validatePayloadSize(payload);
        return payload;
    }

    public String serializeActionParams(Map<String, Object> actionParams) {
        try {
            return objectMapper.writeValueAsString(actionParams);
        } catch (JsonProcessingException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private void validateText(String value) {
        if (value == null || value.isBlank()) {
            throw new GlobalException(INVALID_INPUT);
        }
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
