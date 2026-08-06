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
import devkor.com.teamcback.domain.notification.entity.type.SurveyPushScheduleStatus;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.SurveyPushScheduleRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyPushScheduleWorkerTest {

    private static final String SURVEY_KEY = "fall-2026";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private SurveyPushScheduleRepository surveyPushScheduleRepository;

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchService pushDispatchService;

    @Mock
    private PushEventFlagService pushEventFlagService;

    private SurveyPushScheduleWorker worker;

    @BeforeEach
    void setUp() {
        worker = new SurveyPushScheduleWorker(
                surveyPushScheduleRepository,
                pushInstallationRepository,
                pushDispatchService,
                pushEventFlagService,
                FIXED_CLOCK
        );
    }

    @Test
    void surveyFlagFalseDoesNotClaimOrEnqueue() {
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(false);

        int processed = worker.processDueSchedulesOnce();

        assertThat(processed).isZero();
        verify(surveyPushScheduleRepository, never()).findDuePendingForUpdateSkipLocked(any(), any(Integer.class));
        verify(pushDispatchService, never()).enqueue(any());
    }

    @Test
    void dueStartedScheduleEnqueuesAllProductionActualGeneralAndCompletes() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.STARTED, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(true);

        worker.processDueSchedulesOnce();

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        PushDispatchCommand command = captor.getValue();
        assertThat(command.notificationType()).isEqualTo(NotificationType.GENERAL);
        assertThat(command.mode()).isEqualTo(PushMode.ACTUAL);
        assertThat(command.appVariant()).isEqualTo(AppVariant.PRODUCTION);
        assertThat(command.targetType()).isEqualTo(PushTargetType.ALL);
        assertThat(command.targetValue()).isEqualTo("ALL");
        assertThat(command.actionType()).isEqualTo(PushActionType.HOME);
        assertThat(command.actionParams()).isEmpty();
        assertThat(command.idempotencyKey()).isEqualTo("survey:" + SURVEY_KEY + ":STARTED");
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
        assertThat(schedule.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
    }

    @Test
    void dueReminderScheduleEnqueuesUserTarget() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100)));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-18T10:00:00", 100)));
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION)).thenReturn(true);

        worker.processDueSchedulesOnce();

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().targetType()).isEqualTo(PushTargetType.USER);
        assertThat(captor.getValue().targetValue()).isEqualTo("7");
        assertThat(captor.getValue().title()).isEqualTo("잠깐, 설문을 잊지 않으셨나요?");
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
    }

    @Test
    void noActiveTargetMarksSkippedWithProcessedAt() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(false);

        worker.processDueSchedulesOnce();

        verify(pushDispatchService, never()).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.SKIPPED);
        assertThat(schedule.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
    }

    @Test
    void pushDispatchInvalidInputNotCausedByNoTargetKeepsPending() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(true);
        when(pushDispatchService.enqueue(any())).thenThrow(new GlobalException(ResultCode.INVALID_INPUT));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.PENDING);
        assertThat(schedule.getProcessedAt()).isNull();
    }

    @Test
    void pushDispatchGlobalExceptionKeepsPending() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(true);
        when(pushDispatchService.enqueue(any())).thenThrow(new GlobalException(ResultCode.UNSUPPORTED_REQUEST));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.PENDING);
        assertThat(schedule.getProcessedAt()).isNull();
    }

    @Test
    void pushDispatchRuntimeExceptionKeepsPending() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(true);
        when(pushDispatchService.enqueue(any())).thenThrow(new RuntimeException("boom"));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.PENDING);
        assertThat(schedule.getProcessedAt()).isNull();
    }

    @Test
    void reminderIsCancelledWhenLatestD3DateHasPriority() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100)));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-17T10:00:00", 100)));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService, never()).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
    }

    @Test
    void reminderIsCancelledWhenLatestDeadlineDateHasPriority() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T10:00:00", 100)));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService, never()).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
        assertThat(schedule.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
    }

    @Test
    void reminderIsCancelledWhenNowIsAfterDeadline() {
        SurveyPushSchedule schedule = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-16T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(schedule));
        when(surveyPushScheduleRepository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:59:59", 100)));

        worker.processDueSchedulesOnce();

        verify(pushDispatchService, never()).enqueue(any());
        assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
        assertThat(schedule.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
    }

    @Test
    void deadlineAndD3SchedulesDoNotRunReminderPriorityCancellation() {
        SurveyPushSchedule started = schedule(SurveyNotificationStage.STARTED, null, "2026-08-17T09:00:00", 100);
        SurveyPushSchedule d3 = schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-17T09:00:00", 100);
        SurveyPushSchedule deadline = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-17T09:00:00", 100);
        when(pushEventFlagService.isEnabled(PushEventType.SURVEY)).thenReturn(true);
        when(surveyPushScheduleRepository.findDuePendingForUpdateSkipLocked(LocalDateTime.parse("2026-08-17T10:00:00"), 50))
                .thenReturn(List.of(started, d3, deadline));
        when(pushInstallationRepository.existsByAppVariantAndActiveTrue(AppVariant.PRODUCTION)).thenReturn(true);

        worker.processDueSchedulesOnce();

        verify(surveyPushScheduleRepository, never()).findBySurveyKeyAndNotificationStage(any(), any());
        verify(pushDispatchService, times(3)).enqueue(any());
        assertThat(started.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
        assertThat(d3.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
        assertThat(deadline.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
    }

    private SurveyPushSchedule schedule(
            SurveyNotificationStage stage,
            Long targetUserId,
            String scheduledAt,
            int rewardPoint
    ) {
        return new SurveyPushSchedule(
                SURVEY_KEY,
                stage,
                targetUserId,
                LocalDateTime.parse(scheduledAt),
                rewardPoint,
                SurveyPushScheduleService.idempotencyKey(SURVEY_KEY, stage, targetUserId),
                LocalDateTime.parse("2026-08-06T10:00:00")
        );
    }
}
