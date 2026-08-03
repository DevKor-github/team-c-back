package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.client.ExpoPushClient;
import devkor.com.teamcback.domain.notification.client.ExpoPushClientException;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushResponse;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushTicket;
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
import static devkor.com.teamcback.global.response.ResultCode.EXPO_PUSH_RETRYABLE_ERROR;
import static devkor.com.teamcback.global.response.ResultCode.EXPO_PUSH_TICKET_ERROR;
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
    private static final String DEFAULT_SOUND = "default";
    private static final String TICKET_STATUS_OK = "ok";
    private static final String CLIENT_ERROR_STATUS = "client_error";
    private static final String RETRYABLE_ERROR = "retryable";

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushPayloadFactory pushPayloadFactory;
    private final ExpoPushClient expoPushClient;
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
                .orElseGet(() -> createAndSend(
                        userId,
                        idempotencyKey,
                        installation
                ));
    }

    private NotificationTestRes createAndSend(
            Long userId,
            String idempotencyKey,
            PushInstallation installation
    ) {
        String notificationId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);

        PushDispatch dispatch;
        PushMessage message;

        try {
            dispatch = pushDispatchRepository.saveAndFlush(new PushDispatch(
                    NotificationType.GENERAL,
                    PushMode.TEST,
                    installation.getAppVariant(),
                    PushTargetType.INSTALLATION,
                    installation.getInstallationId(),
                    TEST_TITLE,
                    TEST_BODY,
                    PushActionType.TEST,
                    notificationId,
                    idempotencyKey,
                    userId,
                    now
            ));

            message = pushMessageRepository.saveAndFlush(new PushMessage(
                    dispatch,
                    installation,
                    now
            ));

            dispatch.updateRecipientCount(1);
            pushDispatchRepository.save(dispatch);
        } catch (DataIntegrityViolationException e) {
            return pushDispatchRepository.findByIdempotencyKey(idempotencyKey)
                    .map(dispatchFromRace -> responseFromExistingDispatch(dispatchFromRace, installation))
                    .orElseThrow(() -> new GlobalException(EXPO_PUSH_NON_RETRYABLE_ERROR));
        }

        try {
            ExpoPushResponse response = expoPushClient.send(List.of(new ExpoPushRequest(
                    installation.getExpoPushToken(),
                    TEST_TITLE,
                    TEST_BODY,
                    DEFAULT_SOUND,
                    pushPayloadFactory.create(
                            notificationId,
                            TEST_TITLE,
                            TEST_BODY,
                            PushMode.TEST,
                            installation.getAppVariant(),
                            PushActionType.TEST,
                            Collections.emptyMap()
                    ).data()
            )));

            ExpoPushTicket ticket = response.data().get(0);
            message.recordTicket(
                    ticket.status(),
                    ticket.id(),
                    ticket.details() == null ? null : ticket.details().error(),
                    LocalDateTime.now(clock)
            );
            pushMessageRepository.save(message);

            if (!TICKET_STATUS_OK.equals(ticket.status())) {
                throw new GlobalException(EXPO_PUSH_TICKET_ERROR);
            }

            return new NotificationTestRes(
                    notificationId,
                    installation.getInstallationId(),
                    installation.getAppVariant(),
                    ticket.status(),
                    ticket.id()
            );
        } catch (ExpoPushClientException e) {
            message.recordClientError(
                    e.isRetryable(),
                    LocalDateTime.now(clock)
            );
            pushMessageRepository.save(message);
            throw new GlobalException(e.isRetryable()
                    ? EXPO_PUSH_RETRYABLE_ERROR
                    : EXPO_PUSH_NON_RETRYABLE_ERROR);
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

        if (message.getTicketStatus() == null) {
            throw new GlobalException(EXPO_PUSH_RETRYABLE_ERROR);
        }

        if (CLIENT_ERROR_STATUS.equals(message.getTicketStatus())) {
            throw new GlobalException(RETRYABLE_ERROR.equals(message.getTicketError())
                    ? EXPO_PUSH_RETRYABLE_ERROR
                    : EXPO_PUSH_NON_RETRYABLE_ERROR);
        }

        if (!TICKET_STATUS_OK.equals(message.getTicketStatus())) {
            throw new GlobalException(EXPO_PUSH_TICKET_ERROR);
        }

        return new NotificationTestRes(
                dispatch.getActionParams(),
                message.getInstallationId(),
                dispatch.getAppVariant(),
                message.getTicketStatus(),
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
