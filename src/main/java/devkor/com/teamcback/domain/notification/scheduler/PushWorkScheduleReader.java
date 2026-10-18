package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.entity.type.PushAudience;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.domain.notification.repository.SurveyPushScheduleRepository;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushWorkScheduleReader {
    private final PushMessageRepository messages;
    private final SurveyPushScheduleRepository surveys;
    private final PushEventFlagService flags;
    @Value("${push.storage:mysql}") private String storage;
    @Value("${push.audience:INTERNAL_TEST}") private String audience;

    @Transactional(transactionManager = "pushTransactionManager", readOnly = true)
    public LocalDateTime next(PushWorkSignals.Work work) {
        return switch (work) {
            case DISPATCH -> messages.findNextQueuedAt();
            case RECEIPT -> messages.findNextReceiptAt();
            case SURVEY -> flags.isEnabled(PushEventType.SURVEY)
                    ? surveys.findNextPendingAt("neon".equals(storage)
                            ? PushAudience.valueOf(audience) : PushAudience.LEGACY_UNKNOWN) : null;
        };
    }
}
