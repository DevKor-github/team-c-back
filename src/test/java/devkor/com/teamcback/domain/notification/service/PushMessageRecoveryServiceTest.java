package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushRecoveryWorkerProperties;
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
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushMessageRecoveryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private PushMessageRepository pushMessageRepository;

    @Mock
    private PushDispatchRepository pushDispatchRepository;

    @Test
    void staleSendingWithoutTicketFailsWithWorkerInterrupted() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = message(dispatch, installation);
        message.markSending(LocalDateTime.now(CLOCK).minusMinutes(31));

        stubStaleMessages(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.FAILED, 1L)));

        int recoveredCount = service(30, 100).recoverStaleSendingMessages();

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(message.getTicketStatus()).isEqualTo("worker_interrupted");
        assertThat(message.getTicketError()).isEqualTo("worker_interrupted");
        assertThat(message.getReceiptAvailableAt()).isNull();
        assertThat(dispatch.getStatus()).isEqualTo(PushDispatchStatus.FAILED);
    }

    @Test
    void staleSendingWithTicketRecoversToReceiptPendingWithoutResend() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = message(dispatch, installation);
        message.recordTicket("ok", "ticket-1", null, LocalDateTime.now(CLOCK).minusMinutes(40));
        message.markReceiptChecking(LocalDateTime.now(CLOCK).minusMinutes(31));
        ReflectionTestUtils.setField(message, "receiptAvailableAt", null);

        stubStaleMessages(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.RECEIPT_PENDING, 1L)));

        int recoveredCount = service(30, 100).recoverStaleSendingMessages();

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.RECEIPT_PENDING);
        assertThat(message.getExpoTicketId()).isEqualTo("ticket-1");
        assertThat(message.getReceiptAvailableAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(dispatch.getStatus()).isEqualTo(PushDispatchStatus.PROCESSING);
    }

    @Test
    void nonStaleSendingIsNotChangedWhenQueryDoesNotReturnIt() {
        PushMessage message = message(dispatch(), installation());
        message.markSending(LocalDateTime.now(CLOCK).minusMinutes(29));
        when(pushMessageRepository.findStaleSendingForUpdateSkipLocked(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of());

        int recoveredCount = service(30, 100).recoverStaleSendingMessages();

        assertThat(recoveredCount).isZero();
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.SENDING);
    }

    @Test
    void nonSendingMessageIsNotChangedWhenQueryDoesNotReturnIt() {
        PushMessage message = message(dispatch(), installation());
        when(pushMessageRepository.findStaleSendingForUpdateSkipLocked(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of());

        int recoveredCount = service(30, 100).recoverStaleSendingMessages();

        assertThat(recoveredCount).isZero();
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.QUEUED);
    }

    @Test
    void recoveryUsesConfiguredStaleThresholdAndBatchSize() {
        when(pushMessageRepository.findStaleSendingForUpdateSkipLocked(any(LocalDateTime.class), eq(7)))
                .thenReturn(List.of());

        service(45, 7).recoverStaleSendingMessages();

        ArgumentCaptor<LocalDateTime> staleBeforeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pushMessageRepository).findStaleSendingForUpdateSkipLocked(staleBeforeCaptor.capture(), eq(7));
        assertThat(staleBeforeCaptor.getValue()).isEqualTo(LocalDateTime.now(CLOCK).minusMinutes(45));
    }

    private PushMessageRecoveryService service(
            long staleThresholdMinutes,
            int batchSize
    ) {
        return new PushMessageRecoveryService(
                pushMessageRepository,
                pushDispatchRepository,
                new PushRecoveryWorkerProperties(true, "0 0 4 * * *", staleThresholdMinutes, batchSize),
                CLOCK
        );
    }

    private void stubStaleMessages(List<PushMessage> messages) {
        when(pushMessageRepository.findStaleSendingForUpdateSkipLocked(any(LocalDateTime.class), eq(100)))
                .thenReturn(messages);
    }

    private void stubStatusCounts(
            PushDispatch dispatch,
            List<PushMessageRepository.PushDispatchMessageStatusCount> counts
    ) {
        when(pushMessageRepository.countStatusesByDispatchIds(any(Collection.class)))
                .thenReturn(counts);
        when(pushDispatchRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(dispatch));
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
                LocalDateTime.now(CLOCK)
        );
        ReflectionTestUtils.setField(dispatch, "pushDispatchId", 10L);
        dispatch.updateRecipientCount(1);
        return dispatch;
    }

    private PushMessage message(
            PushDispatch dispatch,
            PushInstallation installation
    ) {
        PushMessage message = new PushMessage(
                dispatch,
                installation,
                LocalDateTime.now(CLOCK)
        );
        ReflectionTestUtils.setField(message, "pushMessageId", 20L);
        return message;
    }

    private static class StatusCount implements PushMessageRepository.PushDispatchMessageStatusCount {

        private final Long dispatchId;
        private final PushMessageStatus status;
        private final long count;

        private StatusCount(
                Long dispatchId,
                PushMessageStatus status,
                long count
        ) {
            this.dispatchId = dispatchId;
            this.status = status;
            this.count = count;
        }

        @Override
        public Long getDispatchId() {
            return dispatchId;
        }

        @Override
        public PushMessageStatus getStatus() {
            return status;
        }

        @Override
        public long getCount() {
            return count;
        }
    }
}
