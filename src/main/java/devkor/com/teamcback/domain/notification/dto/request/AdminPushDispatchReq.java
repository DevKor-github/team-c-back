package devkor.com.teamcback.domain.notification.dto.request;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import java.util.List;
import java.util.Map;

public record AdminPushDispatchReq(
        PushMode mode,
        AppVariant appVariant,
        PushTargetType targetType,
        String targetValue,
        List<String> targetValues,
        String title,
        String body,
        String imageUrl,
        PushActionType actionType,
        Map<String, Object> actionParams,
        Boolean confirm
) {

    public AdminPushDispatchReq(
            PushMode mode,
            AppVariant appVariant,
            PushTargetType targetType,
            String targetValue,
            String title,
            String body,
            PushActionType actionType,
            Map<String, Object> actionParams,
            Boolean confirm
    ) {
        this(
                mode,
                appVariant,
                targetType,
                targetValue,
                null,
                title,
                body,
                null,
                actionType,
                actionParams,
                confirm
        );
    }

    public AdminPushDispatchReq(
            PushMode mode,
            AppVariant appVariant,
            PushTargetType targetType,
            String targetValue,
            List<String> targetValues,
            String title,
            String body,
            PushActionType actionType,
            Map<String, Object> actionParams,
            Boolean confirm
    ) {
        this(
                mode,
                appVariant,
                targetType,
                targetValue,
                targetValues,
                title,
                body,
                null,
                actionType,
                actionParams,
                confirm
        );
    }
}
