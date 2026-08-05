package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushRecoveryWorkerProperties;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushMessageRecoveryService {

    private static final String WORKER_INTERRUPTED = "worker_interrupted";

    private final PushMessageRepository pushMessageRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushRecoveryWorkerProperties pushRecoveryWorkerProperties;
    private final Clock clock;

    @Transactional
    public int recoverStaleSendingMessages() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleBefore = now.minusMinutes(pushRecoveryWorkerProperties.staleThresholdMinutes());
        List<PushMessage> messages = pushMessageRepository.findStaleSendingForUpdateSkipLocked(
                staleBefore,
                pushRecoveryWorkerProperties.batchSize()
        );

        if (messages.isEmpty()) {
            return 0;
        }

        Set<Long> dispatchIds = new HashSet<>();
        messages.forEach(message -> {
            dispatchIds.add(message.getDispatch().getPushDispatchId());
            if (hasText(message.getExpoTicketId())) {
                message.recoverInterruptedSendingWithTicket(now);
                return;
            }
            message.recoverInterruptedSendingWithoutTicket(WORKER_INTERRUPTED, now);
        });

        refreshDispatchStatuses(dispatchIds, now);
        return messages.size();
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
                .forEach(dispatch -> updateDispatchStatus(dispatch, countsByDispatchId, now));
    }

    private void updateDispatchStatus(
            PushDispatch dispatch,
            Map<Long, EnumMap<PushMessageStatus, Long>> countsByDispatchId,
            LocalDateTime now
    ) {
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
    }

    private long count(
            EnumMap<PushMessageStatus, Long> counts,
            PushMessageStatus status
    ) {
        return counts.getOrDefault(status, 0L);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
