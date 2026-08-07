package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.client.ExpoPushClient;
import devkor.com.teamcback.domain.notification.dto.request.NotificationTestReq;
import devkor.com.teamcback.domain.notification.dto.response.NotificationTestRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTestServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchRepository pushDispatchRepository;

    @Mock
    private PushMessageRepository pushMessageRepository;

    @Mock
    private PushPayloadFactory pushPayloadFactory;

    @Test
    void sendTestEnqueuesQueuedMessageWithoutExpoClientDependency() {
        PushInstallation installation = installation();
        when(pushInstallationRepository.findByInstallationId("install-1"))
                .thenReturn(Optional.of(installation));
        when(pushDispatchRepository.findByIdempotencyKey("7b347ad7-6138-4cb7-af7d-f5201703a596"))
                .thenReturn(Optional.empty());
        when(pushPayloadFactory.serializeActionParams(any()))
                .thenReturn("{}");
        when(pushDispatchRepository.saveAndFlush(any(PushDispatch.class)))
                .thenAnswer(invocation -> {
                    PushDispatch dispatch = invocation.getArgument(0);
                    ReflectionTestUtils.setField(dispatch, "pushDispatchId", 10L);
                    return dispatch;
                });
        when(pushMessageRepository.saveAndFlush(any(PushMessage.class)))
                .thenAnswer(invocation -> {
                    PushMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "pushMessageId", 20L);
                    return message;
                });

        NotificationTestService service = service();
        NotificationTestRes response = service.sendTest(
                1L,
                "7b347ad7-6138-4cb7-af7d-f5201703a596",
                new NotificationTestReq(1, "install-1")
        );

        assertThat(response.notificationId()).isEqualTo("20");
        assertThat(response.messageStatus()).isEqualTo(PushMessageStatus.QUEUED);
        assertThat(response.ticketId()).isNull();
        assertThat(hasExpoPushClientField()).isFalse();
    }

    @Test
    void sendTestReturnsExistingDispatchMessageForSameIdempotencyKey() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = message(dispatch, installation);
        ReflectionTestUtils.setField(message, "pushMessageId", 20L);

        when(pushInstallationRepository.findByInstallationId("install-1"))
                .thenReturn(Optional.of(installation));
        when(pushDispatchRepository.findByIdempotencyKey("7b347ad7-6138-4cb7-af7d-f5201703a596"))
                .thenReturn(Optional.of(dispatch));
        when(pushMessageRepository.findAllByDispatch(dispatch))
                .thenReturn(List.of(message));

        NotificationTestRes response = service().sendTest(
                1L,
                "7b347ad7-6138-4cb7-af7d-f5201703a596",
                new NotificationTestReq(1, "install-1")
        );

        assertThat(response.notificationId()).isEqualTo("20");
        assertThat(response.messageStatus()).isEqualTo(PushMessageStatus.QUEUED);
        verify(pushDispatchRepository, never()).saveAndFlush(any(PushDispatch.class));
        verify(pushMessageRepository, never()).saveAndFlush(any(PushMessage.class));
    }

    @Test
    void receiptPendingDoesNotCompleteDispatch() {
        PushDispatch dispatch = dispatch();
        dispatch.updateRecipientCount(1);

        dispatch.updateStatusFromMessageSummary(
                1,
                0,
                0,
                0,
                java.time.LocalDateTime.now(CLOCK)
        );

        assertThat(dispatch.getStatus()).isEqualTo(PushDispatchStatus.PROCESSING);
        assertThat(dispatch.getCompletedAt()).isNull();
    }

    private NotificationTestService service() {
        return new NotificationTestService(
                pushInstallationRepository,
                pushDispatchRepository,
                pushMessageRepository,
                pushPayloadFactory,
                CLOCK
        );
    }

    private PushInstallation installation() {
        PushInstallation installation = new PushInstallation(
                1L,
                "install-1",
                "ExponentPushToken[token]",
                AppVariant.DEV
        );
        ReflectionTestUtils.setField(installation, "pushInstallationId", 100L);
        return installation;
    }

    private PushDispatch dispatch() {
        PushDispatch dispatch = new PushDispatch(
                NotificationType.GENERAL,
                PushMode.TEST,
                AppVariant.DEV,
                PushTargetType.INSTALLATION,
                "install-1",
                "title",
                "body",
                PushActionType.TEST,
                "{}",
                "7b347ad7-6138-4cb7-af7d-f5201703a596",
                1L,
                java.time.LocalDateTime.now(CLOCK)
        );
        ReflectionTestUtils.setField(dispatch, "pushDispatchId", 10L);
        return dispatch;
    }

    private PushMessage message(
            PushDispatch dispatch,
            PushInstallation installation
    ) {
        return new PushMessage(
                dispatch,
                installation,
                java.time.LocalDateTime.now(CLOCK)
        );
    }

    private boolean hasExpoPushClientField() {
        return Arrays.stream(NotificationTestService.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(ExpoPushClient.class::equals);
    }
}
