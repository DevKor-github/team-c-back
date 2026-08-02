package devkor.com.teamcback.domain.notification.validation;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

@Component
public class PushActionValidator {

    private static final Set<PushActionType> ACTIONS_WITHOUT_PARAMS = Set.of(
            PushActionType.HOME,
            PushActionType.NOTICE,
            PushActionType.MY_PAGE,
            PushActionType.TEST
    );

    public Map<String, Object> validateAndNormalize(
            PushActionType actionType,
            AppVariant appVariant,
            Map<String, Object> params
    ) {
        if (actionType == null || appVariant == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;

        if (PushActionType.TEST.equals(actionType)
                && AppVariant.PRODUCTION.equals(appVariant)) {
            throw new GlobalException(INVALID_INPUT);
        }

        if (ACTIONS_WITHOUT_PARAMS.contains(actionType)) {
            validateNoParams(safeParams);
            return Collections.emptyMap();
        }

        return switch (actionType) {
            case BUS_STOP -> validateSinglePositiveLong(safeParams, "stopId");
            case BUILDING_DETAIL -> validateSinglePositiveLong(safeParams, "buildingId");
            case PLACE_DETAIL -> validateSinglePositiveLong(safeParams, "placeId");
            default -> throw new GlobalException(INVALID_INPUT);
        };
    }

    private void validateNoParams(Map<String, Object> params) {
        if (!params.isEmpty()) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private Map<String, Object> validateSinglePositiveLong(
            Map<String, Object> params,
            String requiredKey
    ) {
        if (params.size() != 1 || !params.containsKey(requiredKey)) {
            throw new GlobalException(INVALID_INPUT);
        }

        Long value = parsePositiveLong(params.get(requiredKey));

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put(requiredKey, value);
        return normalized;
    }

    private Long parsePositiveLong(Object value) {
        Long parsed;

        if (value instanceof Integer integerValue) {
            parsed = integerValue.longValue();
        } else if (value instanceof Long longValue) {
            parsed = longValue;
        } else if (value instanceof String stringValue) {
            parsed = parseString(stringValue);
        } else {
            throw new GlobalException(INVALID_INPUT);
        }

        if (parsed <= 0) {
            throw new GlobalException(INVALID_INPUT);
        }

        return parsed;
    }

    private Long parseString(String value) {
        if (value == null || value.isBlank()) {
            throw new GlobalException(INVALID_INPUT);
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }
}
