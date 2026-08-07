package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.SurveyPushSchedule;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.entity.type.SurveyNotificationStage;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.SurveyPushScheduleRepository;
import devkor.com.teamcback.domain.notification.template.DomainPushContentFactory;
import devkor.com.teamcback.domain.notification.template.PushContent;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SurveyPushScheduleWorker {

    private static final int BATCH_SIZE = 50;
    private static final Long SYSTEM_CREATED_BY = 0L;
    private static final String ALL_TARGET_VALUE = "ALL";

    private final SurveyPushScheduleRepository surveyPushScheduleRepository;
    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchService pushDispatchService;
    private final PushEventFlagService pushEventFlagService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${push.survey.poll-interval-ms:60000}")
    @Transactional
    public void processDueSchedules() {
        processDueSchedulesOnce();
    }

    public int processDueSchedulesOnce() {
        if (!pushEventFlagService.isEnabled(PushEventType.SURVEY)) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<SurveyPushSchedule> schedules = surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(
                now,
                BATCH_SIZE
        );

        schedules.forEach(schedule -> processSchedule(schedule, now));
        return schedules.size();
    }

    private void processSchedule(
            SurveyPushSchedule schedule,
            LocalDateTime now
    ) {
        if (SurveyNotificationStage.REMIND_AFTER_LATER.equals(schedule.getNotificationStage())
                && cancelReminderByLatestPriority(schedule, now)) {
            return;
        }

        List<AppVariant> activeTargetVariants = activeTargetVariants(schedule);
        if (activeTargetVariants.isEmpty()) {
            schedule.skip(now);
            return;
        }

        try {
            activeTargetVariants.forEach(appVariant -> pushDispatchService.enqueue(command(schedule, appVariant)));
            schedule.complete(now);
        } catch (GlobalException e) {
            log.warn(
                    "Survey push schedule processing failed. scheduleId={}, stage={}, resultCode={}",
                    schedule.getSurveyPushScheduleId(),
                    schedule.getNotificationStage(),
                    e.getResultCode()
            );
        } catch (RuntimeException e) {
            log.warn(
                    "Unexpected survey push schedule processing failure. scheduleId={}, stage={}, exception={}",
                    schedule.getSurveyPushScheduleId(),
                    schedule.getNotificationStage(),
                    e.getClass().getSimpleName()
            );
        }
    }

    private boolean cancelReminderByLatestPriority(
            SurveyPushSchedule schedule,
            LocalDateTime now
    ) {
        // 현재 서버에는 설문 참여 완료 상태를 확인할 도메인이 없어 미참여 조건은 후속 설문 기능 연동이 필요하다.
        LocalDate executionDate = schedule.getScheduledAt().toLocalDate();

        SurveyPushSchedule deadline = surveyPushScheduleRepository
                .findBySurveyKeyAndNotificationStage(schedule.getSurveyKey(), SurveyNotificationStage.DEADLINE)
                .orElse(null);
        if (deadline == null) {
            schedule.cancel(now);
            return true;
        }

        if (executionDate.equals(deadline.getScheduledAt().toLocalDate())
                || now.isAfter(deadline.getScheduledAt())) {
            schedule.cancel(now);
            return true;
        }

        return surveyPushScheduleRepository
                .findBySurveyKeyAndNotificationStage(schedule.getSurveyKey(), SurveyNotificationStage.D_MINUS_3)
                .map(SurveyPushSchedule::getScheduledAt)
                .map(LocalDateTime::toLocalDate)
                .filter(executionDate::equals)
                .map(ignored -> {
                    schedule.cancel(now);
                    return true;
                })
                .orElse(false);
    }

    private PushDispatchCommand command(
            SurveyPushSchedule schedule,
            AppVariant appVariant
    ) {
        PushContent content = content(schedule);

        return new PushDispatchCommand(
                NotificationType.GENERAL,
                PushMode.ACTUAL,
                appVariant,
                targetType(schedule),
                targetValue(schedule),
                content.title(),
                content.body(),
                PushActionType.HOME,
                Map.of(),
                "%s:%s".formatted(schedule.getIdempotencyKey(), appVariant.name().toLowerCase()),
                SYSTEM_CREATED_BY
        );
    }

    private PushContent content(SurveyPushSchedule schedule) {
        return switch (schedule.getNotificationStage()) {
            case STARTED -> DomainPushContentFactory.surveyStarted();
            case D_MINUS_3 -> DomainPushContentFactory.surveyDMinus3();
            case DEADLINE -> DomainPushContentFactory.surveyDeadline(schedule.getRewardPoint());
            case REMIND_AFTER_LATER -> DomainPushContentFactory.surveyRemindAfterLater(schedule.getRewardPoint());
        };
    }

    private PushTargetType targetType(SurveyPushSchedule schedule) {
        if (SurveyNotificationStage.REMIND_AFTER_LATER.equals(schedule.getNotificationStage())) {
            return PushTargetType.USER;
        }
        return PushTargetType.ALL;
    }

    private String targetValue(SurveyPushSchedule schedule) {
        if (SurveyNotificationStage.REMIND_AFTER_LATER.equals(schedule.getNotificationStage())) {
            return String.valueOf(schedule.getTargetUserId());
        }
        return ALL_TARGET_VALUE;
    }

    private List<AppVariant> activeTargetVariants(SurveyPushSchedule schedule) {
        return targetAppVariants().stream()
                .filter(appVariant -> hasActiveTarget(schedule, appVariant))
                .toList();
    }

    private boolean hasActiveTarget(
            SurveyPushSchedule schedule,
            AppVariant appVariant
    ) {
        if (SurveyNotificationStage.REMIND_AFTER_LATER.equals(schedule.getNotificationStage())) {
            return schedule.getTargetUserId() != null
                    && pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(
                    schedule.getTargetUserId(),
                    appVariant
            );
        }

        return pushInstallationRepository.existsByAppVariantAndActiveTrue(appVariant);
    }

    private List<AppVariant> targetAppVariants() {
        List<AppVariant> configuredVariants = pushEventFlagService.getTargetAppVariants();
        return configuredVariants == null || configuredVariants.isEmpty()
                ? List.of(AppVariant.PRODUCTION)
                : configuredVariants;
    }
}
