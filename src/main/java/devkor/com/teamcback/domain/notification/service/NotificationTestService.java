package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.request.NotificationTestReq;
import devkor.com.teamcback.domain.notification.dto.response.NotificationTestRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static devkor.com.teamcback.global.response.ResultCode.EXPO_PUSH_NON_RETRYABLE_ERROR;
import static devkor.com.teamcback.global.response.ResultCode.FORBIDDEN_PUSH_INSTALLATION;
import static devkor.com.teamcback.global.response.ResultCode.INACTIVE_PUSH_INSTALLATION;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PUSH_INSTALLATION;
import static devkor.com.teamcback.global.response.ResultCode.UNSUPPORTED_PUSH_INSTALLATION_VARIANT;

@Service
@RequiredArgsConstructor
public class NotificationTestService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int SCHEMA_VERSION = 1;
    private static final String TEST_TITLE = "고대로 테스트 알림";
    private static final String TEST_BODY = "푸시 알림 연결이 정상적으로 동작합니다.";

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushPayloadFactory pushPayloadFactory;
    private final Clock clock;

    public NotificationTestRes sendTest(
            Long userId,
            String idempotencyKey,
            NotificationTestReq request
    ) {
        validateRequest(userId, idempotencyKey, request);

        PushInstallation installation = findAndValidateInstallation(
                userId,
                request.installationId()
        );

        return pushDispatchRepository.findByIdempotencyKey(idempotencyKey)
                .map(dispatch -> responseFromExistingDispatch(dispatch, installation))
                .orElseGet(() -> enqueue(
                        userId,
                        idempotencyKey,
                        installation
                ));
    }

    private NotificationTestRes enqueue(
            Long userId,
            String idempotencyKey,
            PushInstallation installation
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        try {
            PushDispatch dispatch = pushDispatchRepository.saveAndFlush(new PushDispatch(
                    NotificationType.GENERAL,
                    PushMode.TEST,
                    installation.getAppVariant(),
                    PushTargetType.INSTALLATION,
                    installation.getInstallationId(),
                    TEST_TITLE,
                    TEST_BODY,
                    PushActionType.TEST,
                    pushPayloadFactory.serializeActionParams(Collections.emptyMap()),
                    idempotencyKey,
                    userId,
                    now
            ));

            PushMessage message = pushMessageRepository.saveAndFlush(new PushMessage(
                    dispatch,
                    installation,
                    now
            ));

            dispatch.updateRecipientCount(1);
            pushDispatchRepository.save(dispatch);

            return response(dispatch, message);
        } catch (DataIntegrityViolationException e) {
            return pushDispatchRepository.findByIdempotencyKey(idempotencyKey)
                    .map(dispatchFromRace -> responseFromExistingDispatch(dispatchFromRace, installation))
                    .orElseThrow(() -> new GlobalException(EXPO_PUSH_NON_RETRYABLE_ERROR));
        }
    }

    private NotificationTestRes responseFromExistingDispatch(
            PushDispatch dispatch,
            PushInstallation installation
    ) {
        List<PushMessage> messages = pushMessageRepository.findAllByDispatch(dispatch);

        if (messages.size() != 1) {
            throw new GlobalException(EXPO_PUSH_NON_RETRYABLE_ERROR);
        }

        PushMessage message = messages.get(0);

        if (!installation.getInstallationId().equals(message.getInstallationId())) {
            throw new GlobalException(INVALID_INPUT);
        }

        return response(dispatch, message);
    }

    private NotificationTestRes response(
            PushDispatch dispatch,
            PushMessage message
    ) {
        return new NotificationTestRes(
                String.valueOf(message.getPushMessageId()),
                message.getInstallationId(),
                dispatch.getAppVariant(),
                message.getStatus(),
                message.getExpoTicketId()
        );
    }

    private PushInstallation findAndValidateInstallation(
            Long userId,
            String installationId
    ) {
        PushInstallation installation = pushInstallationRepository.findByInstallationId(installationId)
                .orElseThrow(() -> new GlobalException(NOT_FOUND_PUSH_INSTALLATION));

        if (!userId.equals(installation.getUserId())) {
            throw new GlobalException(FORBIDDEN_PUSH_INSTALLATION);
        }

        if (!installation.isActive()) {
            throw new GlobalException(INACTIVE_PUSH_INSTALLATION);
        }

        if (AppVariant.PRODUCTION.equals(installation.getAppVariant())) {
            throw new GlobalException(UNSUPPORTED_PUSH_INSTALLATION_VARIANT);
        }

        if (!AppVariant.DEV.equals(installation.getAppVariant())
                && !AppVariant.PREVIEW.equals(installation.getAppVariant())) {
            throw new GlobalException(UNSUPPORTED_PUSH_INSTALLATION_VARIANT);
        }

        if (!hasText(installation.getExpoPushToken())) {
            throw new GlobalException(INVALID_INPUT);
        }

        return installation;
    }

    private void validateRequest(
            Long userId,
            String idempotencyKey,
            NotificationTestReq request
    ) {
        if (userId == null || request == null || request.schemaVersion() == null
                || request.schemaVersion() != SCHEMA_VERSION) {
            throw new GlobalException(INVALID_INPUT);
        }

        validateText(request.installationId(), 64);
        validateText(idempotencyKey, MAX_IDEMPOTENCY_KEY_LENGTH);

        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private void validateText(
            String value,
            int maxLength
    ) {
        if (!hasText(value) || value.length() > maxLength) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
