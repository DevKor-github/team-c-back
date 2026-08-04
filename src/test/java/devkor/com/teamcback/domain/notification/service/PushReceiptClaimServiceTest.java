package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushReceiptWorkerProperties;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushErrorDetails;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushReceipt;
import devkor.com.teamcback.domain.notification.dto.worker.PushReceiptItem;
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
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushReceiptClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private PushMessageRepository pushMessageRepository;

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchRepository pushDispatchRepository;

    @Test
    void claimDueReceiptsMarksDueReceiptPendingMessagesAsSending() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = receiptPendingMessage(dispatch, installation, "ticket-1");

        when(pushMessageRepository.findDueReceiptPendingForUpdateSkipLocked(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.SENDING, 1L)));

        List<PushReceiptItem> items = service().claimDueReceipts();

        assertThat(items).containsExactly(new PushReceiptItem(20L, 10L, "ticket-1"));
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.SENDING);
    }

    @Test
    void receiptOkMarksDeliveredByTicketIdMap() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage first = receiptPendingMessage(dispatch, installation, "ticket-1");
        PushMessage second = receiptPendingMessage(dispatch, installation, "ticket-2");
        ReflectionTestUtils.setField(second, "pushMessageId", 21L);

        stubMessages(List.of(first, second));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.DELIVERED, 2L)));

        service().recordReceipts(
                List.of(
                        new PushReceiptItem(20L, 10L, "ticket-1"),
                        new PushReceiptItem(21L, 10L, "ticket-2")
                ),
                Map.of(
                        "ticket-2", new ExpoPushReceipt("ok", null, null),
                        "ticket-1", new ExpoPushReceipt("error", "failed", new ExpoPushErrorDetails("MessageTooBig"))
                )
        );

        assertThat(first.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(first.getReceiptError()).isEqualTo("MessageTooBig");
        assertThat(second.getStatus()).isEqualTo(PushMessageStatus.DELIVERED);
        assertThat(second.getReceiptError()).isNull();
    }

    @Test
    void deviceNotRegisteredReceiptFailsMessageAndDeactivatesInstallation() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = receiptPendingMessage(dispatch, installation, "ticket-1");

        stubMessages(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.FAILED, 1L)));
        when(pushInstallationRepository.findById(100L))
                .thenReturn(Optional.of(installation));

        service().recordReceipts(
                List.of(new PushReceiptItem(20L, 10L, "ticket-1")),
                Map.of("ticket-1", new ExpoPushReceipt(
                        "error",
                        null,
                        new ExpoPushErrorDetails("DeviceNotRegistered")
                ))
        );

        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(installation.isActive()).isFalse();
    }

    @Test
    void missingReceiptSchedulesLimitedRetry() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = receiptPendingMessage(dispatch, installation, "ticket-1");

        stubMessages(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.RECEIPT_PENDING, 1L)));

        service().recordReceipts(
                List.of(new PushReceiptItem(20L, 10L, "ticket-1")),
                Map.of()
        );

        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.RECEIPT_PENDING);
        assertThat(message.getReceiptAttempts()).isEqualTo(1);
        assertThat(message.getReceiptAvailableAt()).isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(1));
    }

    @Test
    void maxReceiptAttemptsMarksFailed() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = receiptPendingMessage(dispatch, installation, "ticket-1");
        ReflectionTestUtils.setField(message, "receiptAttempts", 2);

        stubMessages(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.FAILED, 1L)));

        service().recordReceipts(
                List.of(new PushReceiptItem(20L, 10L, "ticket-1")),
                Map.of()
        );

        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(message.getReceiptAvailableAt()).isNull();
    }

    @Test
    void sentMoreThanTwentyFourHoursAgoExpiresWithoutReceiptRequestItem() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = receiptPendingMessage(dispatch, installation, "ticket-1");
        ReflectionTestUtils.setField(message, "sentAt", LocalDateTime.now(CLOCK).minusHours(25));

        when(pushMessageRepository.findDueReceiptPendingForUpdateSkipLocked(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(message));
        stubStatusCounts(dispatch, List.of(new StatusCount(10L, PushMessageStatus.FAILED, 1L)));

        List<PushReceiptItem> items = service().claimDueReceipts();

        assertThat(items).isEmpty();
        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(message.getReceiptError()).isEqualTo("receipt_expired");
    }

    @Test
    void dispatchSummaryUsesOnlyFinalStatusesForCompletion() {
        PushDispatch allDelivered = dispatch();
        allDelivered.updateRecipientCount(2);
        allDelivered.updateStatusFromMessageSummary(0, 0, 2, 0, LocalDateTime.now(CLOCK));
        assertThat(allDelivered.getStatus()).isEqualTo(PushDispatchStatus.COMPLETED);

        PushDispatch partial = dispatch();
        partial.updateRecipientCount(2);
        partial.updateStatusFromMessageSummary(0, 0, 1, 1, LocalDateTime.now(CLOCK));
        assertThat(partial.getStatus()).isEqualTo(PushDispatchStatus.PARTIAL_FAILED);

        PushDispatch failed = dispatch();
        failed.updateRecipientCount(2);
        failed.updateStatusFromMessageSummary(0, 0, 0, 2, LocalDateTime.now(CLOCK));
        assertThat(failed.getStatus()).isEqualTo(PushDispatchStatus.FAILED);

        PushDispatch processing = dispatch();
        processing.updateRecipientCount(2);
        processing.updateStatusFromMessageSummary(1, 0, 1, 0, LocalDateTime.now(CLOCK));
        assertThat(processing.getStatus()).isEqualTo(PushDispatchStatus.PROCESSING);
    }

    private PushReceiptClaimService service() {
        return new PushReceiptClaimService(
                pushMessageRepository,
                pushInstallationRepository,
                pushDispatchRepository,
                new PushReceiptWorkerProperties(true, 1000, 3),
                CLOCK
        );
    }

    private void stubMessages(List<PushMessage> messages) {
        when(pushMessageRepository.findAllByPushMessageIdIn(any(Collection.class)))
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

    private PushMessage receiptPendingMessage(
            PushDispatch dispatch,
            PushInstallation installation,
            String ticketId
    ) {
        PushMessage message = new PushMessage(
                dispatch,
                installation,
                LocalDateTime.now(CLOCK).minusMinutes(20)
        );
        ReflectionTestUtils.setField(message, "pushMessageId", 20L);
        message.recordTicket("ok", ticketId, null, LocalDateTime.now(CLOCK).minusMinutes(20));
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
