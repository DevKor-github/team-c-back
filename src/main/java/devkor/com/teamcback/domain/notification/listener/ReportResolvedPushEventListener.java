package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import devkor.com.teamcback.domain.notification.template.DomainPushContentFactory;
import devkor.com.teamcback.domain.notification.template.PushContent;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportResolvedPushEventListener {

    private static final Long SYSTEM_CREATED_BY = 0L;

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchService pushDispatchService;
    private final PushEventFlagService pushEventFlagService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReportResolvedEvent event) {
        if (!pushEventFlagService.isEnabled(PushEventType.REPORT) || event.reporterUserId() == null) {
            return;
        }

        for (AppVariant targetAppVariant : targetAppVariants()) {
            try {
                if (!pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(
                        event.reporterUserId(),
                        targetAppVariant
                )) {
                    continue;
                }

                PushContent content = DomainPushContentFactory.reportResolved();
                pushDispatchService.enqueue(new PushDispatchCommand(
                        NotificationType.GENERAL,
                        PushMode.ACTUAL,
                        targetAppVariant,
                        PushTargetType.USER,
                        String.valueOf(event.reporterUserId()),
                        content.title(),
                        content.body(),
                        PushActionType.HOME,
                        Map.of(),
                        "report-result:%d:%s:%d:%s".formatted(
                                event.reportId(),
                                event.finalStatus().name(),
                                event.reporterUserId(),
                                targetAppVariant.name().toLowerCase()
                        ),
                        SYSTEM_CREATED_BY
                ));
            } catch (Exception e) {
                log.warn(
                        "report result push failed: reportId={}, reporterUserId={}, finalStatus={}, appVariant={}, error={}",
                        event.reportId(),
                        event.reporterUserId(),
                        event.finalStatus(),
                        targetAppVariant,
                        e.getMessage()
                );
            }
        }
    }

    private List<AppVariant> targetAppVariants() {
        List<AppVariant> configuredVariants = pushEventFlagService.getTargetAppVariants();
        return configuredVariants == null || configuredVariants.isEmpty()
                ? List.of(AppVariant.PRODUCTION)
                : configuredVariants;
    }
}
