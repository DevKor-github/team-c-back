package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushWorkerProperties;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushTicket;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.dto.worker.PushSendItem;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushMessageClaimService {

    private static final String DEFAULT_SOUND = "default";
    private static final String TICKET_STATUS_ERROR = "error";
    private static final String RETRYABLE_TICKET_ERROR = "MessageRateExceeded";
    private static final String DEVICE_NOT_REGISTERED_ERROR = "DeviceNotRegistered";

    private final PushMessageRepository pushMessageRepository;
    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushPayloadFactory pushPayloadFactory;
    private final PushWorkerProperties pushWorkerProperties;
    private final Clock clock;

    @Transactional
    public List<PushSendItem> claimDueMessages() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PushMessage> messages = pushMessageRepository.findDueQueuedForUpdateSkipLocked(
                now,
                pushWorkerProperties.batchSize()
        );

        if (messages.isEmpty()) {
            return List.of();
        }

        Set<Long> dispatchIds = new HashSet<>();
        List<PushSendItem> sendItems = messages.stream()
                .peek(message -> {
                    message.markSending(now);
                    dispatchIds.add(message.getDispatch().getPushDispatchId());
                })
                .map(message -> createSendItem(message, now))
                .filter(item -> item != null)
                .toList();

        refreshDispatchStatuses(dispatchIds, now);
        return sendItems;
    }

    @Transactional
    public void recordTickets(
            List<PushSendItem> items,
            List<ExpoPushTicket> tickets
    ) {
        if (items.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextRetryAt = now.plusNanos(pushWorkerProperties.retryDelayMs() * 1_000_000L);
        Map<Long, PushMessage> messageMap = findMessageMap(items);
        Set<Long> dispatchIds = new HashSet<>();

        for (int i = 0; i < items.size(); i += 1) {
            PushSendItem item = items.get(i);
            PushMessage message = messageMap.get(item.pushMessageId());
            if (message == null) {
                continue;
            }

            ExpoPushTicket ticket = tickets == null || i >= tickets.size() ? null : tickets.get(i);
            String ticketError = ticketError(ticket);
            if (isDeviceNotRegisteredTicket(ticket)) {
                message.recordTicket(
                        ticket.status(),
                        ticket.id(),
                        ticketError,
                        now
                );
                pushInstallationRepository.findById(message.getPushInstallationId())
                        .ifPresent(installation -> installation.deactivate(now));
            } else if (isRetryableTicket(ticket)) {
                message.recordRetryableTicketError(
                        ticket.status(),
                        ticketError,
                        pushWorkerProperties.maxSendAttempts(),
                        nextRetryAt,
                        now
                );
            } else {
                message.recordTicket(
                        ticket == null ? "missing_ticket" : ticket.status(),
                        ticket == null ? null : ticket.id(),
                        ticketError,
                        now
                );
            }
            dispatchIds.add(item.pushDispatchId());
        }

        refreshDispatchStatuses(dispatchIds, now);
    }

    @Transactional
    public void recordClientError(
            List<PushSendItem> items,
            boolean retryable,
            String ticketError
    ) {
        if (items.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextRetryAt = now.plusNanos(pushWorkerProperties.retryDelayMs() * 1_000_000L);
        Map<Long, PushMessage> messageMap = findMessageMap(items);
        Set<Long> dispatchIds = new HashSet<>();

        items.forEach(item -> {
            PushMessage message = messageMap.get(item.pushMessageId());
            if (message == null) {
                return;
            }
            message.recordClientError(
                    retryable,
                    truncate(ticketError),
                    pushWorkerProperties.maxSendAttempts(),
                    nextRetryAt,
                    now
            );
            dispatchIds.add(item.pushDispatchId());
        });

        refreshDispatchStatuses(dispatchIds, now);
    }

    private PushSendItem createSendItem(
            PushMessage message,
            LocalDateTime now
    ) {
        PushDispatch dispatch = message.getDispatch();
        PushInstallation installation = pushInstallationRepository
                .findByPushInstallationIdAndInstallationIdAndAppVariantAndActiveTrue(
                        message.getPushInstallationId(),
                        message.getInstallationId(),
                        dispatch.getAppVariant()
                )
                .orElse(null);

        if (installation == null) {
            message.recordSkipped(
                    "installation_inactive",
                    "inactive_or_variant_mismatch",
                    now
            );
            return null;
        }

        try {
            PushPayload payload = pushPayloadFactory.create(
                    String.valueOf(message.getPushMessageId()),
                    dispatch.getTitle(),
                    dispatch.getBody(),
                    dispatch.getMode(),
                    dispatch.getAppVariant(),
                    dispatch.getActionType(),
                    pushPayloadFactory.deserializeActionParams(dispatch.getActionParams()),
                    dispatch.getImageUrl()
            );

            return new PushSendItem(
                    message.getPushMessageId(),
                    dispatch.getPushDispatchId(),
                    new ExpoPushRequest(
                            installation.getExpoPushToken(),
                            payload.title(),
                            payload.body(),
                            DEFAULT_SOUND,
                            ExpoPushRequest.richContentForImage(payload.image()),
                            payload.data()
                    )
            );
        } catch (RuntimeException e) {
            message.recordSkipped(
                    "invalid_payload",
                    "payload_validation_failed",
                    now
            );
            return null;
        }
    }

    private Map<Long, PushMessage> findMessageMap(List<PushSendItem> items) {
        List<Long> messageIds = items.stream()
                .map(PushSendItem::pushMessageId)
                .toList();

        return pushMessageRepository.findAllByPushMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        PushMessage::getPushMessageId,
                        message -> message
                ));
    }

    private void refreshDispatchStatuses(
            Collection<Long> dispatchIds,
            LocalDateTime now
    ) {
        if (dispatchIds.isEmpty()) {
            return;
        }

        Map<Long, EnumMap<PushMessageStatus, Long>> countsByDispatchId = new HashMap<>();
        pushMessageRepository.countStatusesByDispatchIds(dispatchIds)
                .forEach(count -> countsByDispatchId
                        .computeIfAbsent(
                                count.getDispatchId(),
                                ignored -> new EnumMap<>(PushMessageStatus.class)
                        )
                        .put(count.getStatus(), count.getCount()));

        pushDispatchRepository.findAllById(dispatchIds)
                .forEach(dispatch -> {
                    EnumMap<PushMessageStatus, Long> counts = countsByDispatchId.getOrDefault(
                            dispatch.getPushDispatchId(),
                            new EnumMap<>(PushMessageStatus.class)
                    );
                    dispatch.updateStatusFromMessageSummary(
                            count(counts, PushMessageStatus.QUEUED)
                                    + count(counts, PushMessageStatus.TICKET_RECEIVED)
                                    + count(counts, PushMessageStatus.RECEIPT_PENDING),
                            count(counts, PushMessageStatus.SENDING),
                            count(counts, PushMessageStatus.DELIVERED),
                            count(counts, PushMessageStatus.FAILED),
                            now
                    );
                });
    }

    private long count(
            EnumMap<PushMessageStatus, Long> counts,
            PushMessageStatus status
    ) {
        return counts.getOrDefault(status, 0L);
    }

    private boolean isRetryableTicket(ExpoPushTicket ticket) {
        return ticket != null
                && TICKET_STATUS_ERROR.equals(ticket.status())
                && RETRYABLE_TICKET_ERROR.equals(ticketError(ticket));
    }

    private boolean isDeviceNotRegisteredTicket(ExpoPushTicket ticket) {
        return ticket != null
                && TICKET_STATUS_ERROR.equals(ticket.status())
                && ticket.details() != null
                && DEVICE_NOT_REGISTERED_ERROR.equals(ticket.details().error());
    }

    private String ticketError(ExpoPushTicket ticket) {
        if (ticket == null) {
            return "missing_ticket";
        }

        if (ticket.details() != null && ticket.details().error() != null) {
            return truncate(ticket.details().error());
        }

        return truncate(ticket.message());
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1024) {
            return value;
        }
        return value.substring(0, 1024);
    }
}
