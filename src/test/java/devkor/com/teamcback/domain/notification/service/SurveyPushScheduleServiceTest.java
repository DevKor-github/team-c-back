package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.request.AdminSurveyPushScheduleReq;
import devkor.com.teamcback.domain.notification.dto.response.SurveyReminderRes;
import devkor.com.teamcback.domain.notification.entity.SurveyPushSchedule;
import devkor.com.teamcback.domain.notification.entity.type.SurveyNotificationStage;
import devkor.com.teamcback.domain.notification.entity.type.SurveyPushScheduleStatus;
import devkor.com.teamcback.domain.notification.entity.type.SurveyReminderSuppressedBy;
import devkor.com.teamcback.domain.notification.repository.SurveyPushScheduleRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyPushScheduleServiceTest {

    private static final String SURVEY_KEY = "fall-2026";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-06T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private SurveyPushScheduleRepository repository;

    private SurveyPushScheduleService service;

    @BeforeEach
    void setUp() {
        service = new SurveyPushScheduleService(repository, FIXED_CLOCK);
    }

    @Test
    void upsertAdminSchedulesCreatesThreeWholeAudienceSchedules() {
        List<SurveyPushSchedule> saved = new ArrayList<>();
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(repository.save(any(SurveyPushSchedule.class))).thenAnswer(invocation -> {
            SurveyPushSchedule schedule = invocation.getArgument(0);
            saved.add(schedule);
            return schedule;
        });
        when(repository.findAllBySurveyKeyAndNotificationStageIn(eq(SURVEY_KEY), anyCollection()))
                .thenAnswer(ignored -> saved);

        service.upsertAdminSchedules(
                SURVEY_KEY,
                new AdminSurveyPushScheduleReq(
                        LocalDateTime.parse("2026-08-10T10:00:00"),
                        LocalDateTime.parse("2026-08-20T10:00:00"),
                        100
                )
        );

        assertThat(saved).hasSize(3);
        assertThat(saved)
                .extracting(SurveyPushSchedule::getNotificationStage)
                .containsExactly(
                        SurveyNotificationStage.STARTED,
                        SurveyNotificationStage.D_MINUS_3,
                        SurveyNotificationStage.DEADLINE
                );
        assertThat(saved.get(1).getScheduledAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
        assertThat(saved).allSatisfy(schedule -> {
            assertThat(schedule.getTargetUserId()).isNull();
            assertThat(schedule.getRewardPoint()).isEqualTo(100);
            assertThat(schedule.getStatus()).isEqualTo(SurveyPushScheduleStatus.PENDING);
        });
    }

    @Test
    void upsertAdminSchedulesUpdatesOnlyPendingExistingSchedule() {
        SurveyPushSchedule pending = schedule(SurveyNotificationStage.STARTED, null, "2026-08-10T10:00:00", 100);
        SurveyPushSchedule completed = schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-17T10:00:00", 100);
        completed.complete(LocalDateTime.parse("2026-08-17T10:01:00"));

        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":STARTED"))
                .thenReturn(Optional.of(pending));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":D_MINUS_3"))
                .thenReturn(Optional.of(completed));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":DEADLINE"))
                .thenReturn(Optional.empty());
        when(repository.save(any(SurveyPushSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAllBySurveyKeyAndNotificationStageIn(eq(SURVEY_KEY), anyCollection()))
                .thenReturn(List.of(pending, completed));

        service.upsertAdminSchedules(
                SURVEY_KEY,
                new AdminSurveyPushScheduleReq(
                        LocalDateTime.parse("2026-08-11T10:00:00"),
                        LocalDateTime.parse("2026-08-21T10:00:00"),
                        200
                )
        );

        assertThat(pending.getScheduledAt()).isEqualTo(LocalDateTime.parse("2026-08-11T10:00:00"));
        assertThat(pending.getRewardPoint()).isEqualTo(200);
        assertThat(completed.getScheduledAt()).isEqualTo(LocalDateTime.parse("2026-08-17T10:00:00"));
        assertThat(completed.getRewardPoint()).isEqualTo(100);
        assertThat(completed.getStatus()).isEqualTo(SurveyPushScheduleStatus.COMPLETED);
    }

    @Test
    void upsertAdminSchedulesValidatesTimesAndRewardPoint() {
        assertThatThrownBy(() -> service.upsertAdminSchedules(
                SURVEY_KEY,
                new AdminSurveyPushScheduleReq(
                        LocalDateTime.parse("2026-08-18T10:00:00"),
                        LocalDateTime.parse("2026-08-20T10:00:00"),
                        100
                )
        ))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);

        assertThatThrownBy(() -> service.upsertAdminSchedules(
                SURVEY_KEY,
                new AdminSurveyPushScheduleReq(
                        LocalDateTime.parse("2026-08-10T10:00:00"),
                        LocalDateTime.parse("2026-08-20T10:00:00"),
                        -1
                )
        ))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void remindAfterLaterCreatesOrUpdatesOnePendingPersonalSchedule() {
        SurveyPushSchedule deadline = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100);
        SurveyPushSchedule existing = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-07T09:00:00", 50);

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(deadline));
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.empty());
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.of(existing));

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isTrue();
        assertThat(response.scheduledAt()).isEqualTo(LocalDateTime.parse("2026-08-07T10:00:00"));
        assertThat(existing.getRewardPoint()).isEqualTo(100);
    }

    @Test
    void remindAfterLaterSuppressesByPriorityAndExpiry() {
        assertReminderSuppressed(
                "2026-08-07T10:00:00",
                "2026-08-07T09:00:00",
                SurveyReminderSuppressedBy.DEADLINE
        );
        assertReminderSuppressed(
                "2026-08-20T10:00:00",
                "2026-08-07T09:00:00",
                SurveyReminderSuppressedBy.D3
        );

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-07T09:59:59", 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.empty());

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);
        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.EXPIRED);
    }

    @Test
    void remindAfterLaterCancelsExistingPendingReminderWhenSuppressedByD3() {
        SurveyPushSchedule existing = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-07T09:00:00", 100);

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100)));
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-07T09:00:00", 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.of(existing));

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.D3);
        assertThat(existing.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
        assertThat(existing.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-06T10:00:00"));
        verify(repository, never()).save(any(SurveyPushSchedule.class));
    }

    @Test
    void remindAfterLaterCancelsExistingPendingReminderWhenSuppressedByDeadline() {
        SurveyPushSchedule existing = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-07T09:00:00", 100);

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-07T11:00:00", 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.of(existing));

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.DEADLINE);
        assertThat(existing.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
        assertThat(existing.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-06T10:00:00"));
        verify(repository, never()).save(any(SurveyPushSchedule.class));
    }

    @Test
    void remindAfterLaterCancelsExistingPendingReminderWhenExpired() {
        SurveyPushSchedule existing = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-07T09:00:00", 100);

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-07T09:59:59", 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.of(existing));

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.EXPIRED);
        assertThat(existing.getStatus()).isEqualTo(SurveyPushScheduleStatus.CANCELLED);
        assertThat(existing.getProcessedAt()).isEqualTo(LocalDateTime.parse("2026-08-06T10:00:00"));
        verify(repository, never()).save(any(SurveyPushSchedule.class));
    }

    @Test
    void remindAfterLaterDoesNotCreateCancelledRowWhenSuppressedWithoutExistingReminder() {
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100)));
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.D_MINUS_3, null, "2026-08-07T09:00:00", 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.empty());

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.D3);
        verify(repository, never()).save(any(SurveyPushSchedule.class));
    }

    @Test
    void remindAfterLaterDoesNotRecreateProcessedReminder() {
        SurveyPushSchedule deadline = schedule(SurveyNotificationStage.DEADLINE, null, "2026-08-20T10:00:00", 100);
        SurveyPushSchedule completed = schedule(SurveyNotificationStage.REMIND_AFTER_LATER, 7L, "2026-08-07T09:00:00", 100);
        completed.complete(LocalDateTime.parse("2026-08-07T09:01:00"));

        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(deadline));
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.empty());
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.of(completed));

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.suppressedBy()).isEqualTo(SurveyReminderSuppressedBy.ALREADY_PROCESSED);
    }

    private void assertReminderSuppressed(
            String deadlineAt,
            String d3At,
            SurveyReminderSuppressedBy suppressedBy
    ) {
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.DEADLINE))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.DEADLINE, null, deadlineAt, 100)));
        when(repository.findBySurveyKeyAndNotificationStage(SURVEY_KEY, SurveyNotificationStage.D_MINUS_3))
                .thenReturn(Optional.of(schedule(SurveyNotificationStage.D_MINUS_3, null, d3At, 100)));
        when(repository.findByIdempotencyKey("survey:" + SURVEY_KEY + ":REMIND_AFTER_LATER:7"))
                .thenReturn(Optional.empty());

        SurveyReminderRes response = service.remindAfterLater(SURVEY_KEY, 7L);

        assertThat(response.scheduled()).isFalse();
        assertThat(response.scheduledAt()).isNull();
        assertThat(response.suppressedBy()).isEqualTo(suppressedBy);
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
