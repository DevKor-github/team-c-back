package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushWorkerProperties;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushErrorDetails;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushTicket;
import devkor.com.teamcback.domain.notification.dto.worker.PushSendItem;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushMessageClaimServiceTest {

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

    @Mock
    private PushPayloadFactory pushPayloadFactory;

    @Test
    void ticketOkStoresReceiptAvailableAtFifteenMinutesLater() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = message(dispatch, installation);

        stubRecordTickets(message, dispatch, PushMessageStatus.RECEIPT_PENDING);

        service().recordTickets(
                List.of(new PushSendItem(20L, 10L, null)),
                List.of(new ExpoPushTicket("ok", "ticket-1", null, null))
        );

        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.RECEIPT_PENDING);
        assertThat(message.getExpoTicketId()).isEqualTo("ticket-1");
        assertThat(message.getReceiptAvailableAt())
                .isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(15));
    }

    @Test
    void deviceNotRegisteredTicketFailsMessageAndDeactivatesInstallation() {
        PushInstallation installation = installation();
        PushDispatch dispatch = dispatch();
        PushMessage message = message(dispatch, installation);

        stubRecordTickets(message, dispatch, PushMessageStatus.FAILED);
        when(pushInstallationRepository.findById(100L))
                .thenReturn(Optional.of(installation));

        service().recordTickets(
                List.of(new PushSendItem(20L, 10L, null)),
                List.of(new ExpoPushTicket(
                        "error",
                        null,
                        null,
                        new ExpoPushErrorDetails("DeviceNotRegistered")
                ))
        );

        assertThat(message.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(message.getReceiptAvailableAt()).isNull();
        assertThat(installation.isActive()).isFalse();
    }

    private void stubRecordTickets(
            PushMessage message,
            PushDispatch dispatch,
            PushMessageStatus status
    ) {
        when(pushMessageRepository.findAllByPushMessageIdIn(any(Collection.class)))
                .thenReturn(List.of(message));
        when(pushMessageRepository.countStatusesByDispatchIds(any(Collection.class)))
                .thenReturn(List.of(new StatusCount(10L, status, 1L)));
        when(pushDispatchRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(dispatch));
    }

    private PushMessageClaimService service() {
        return new PushMessageClaimService(
                pushMessageRepository,
                pushInstallationRepository,
                pushDispatchRepository,
                pushPayloadFactory,
                new PushWorkerProperties(true, 100, 3, 30_000L),
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
