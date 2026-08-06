package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.request.AdminSurveyPushScheduleReq;
import devkor.com.teamcback.domain.notification.dto.response.AdminSurveyPushScheduleRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminSurveyPushScheduleStageRes;
import devkor.com.teamcback.domain.notification.dto.response.SurveyReminderRes;
import devkor.com.teamcback.domain.notification.entity.SurveyPushSchedule;
import devkor.com.teamcback.domain.notification.entity.type.SurveyNotificationStage;
import devkor.com.teamcback.domain.notification.entity.type.SurveyReminderSuppressedBy;
import devkor.com.teamcback.domain.notification.repository.SurveyPushScheduleRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyPushScheduleService {

    private static final int MAX_SURVEY_KEY_LENGTH = 64;
    private static final List<SurveyNotificationStage> ADMIN_STAGES = List.of(
            SurveyNotificationStage.STARTED,
            SurveyNotificationStage.D_MINUS_3,
            SurveyNotificationStage.DEADLINE
    );

    private final SurveyPushScheduleRepository surveyPushScheduleRepository;
    private final Clock clock;

    @Transactional
    public AdminSurveyPushScheduleRes upsertAdminSchedules(
            String surveyKey,
            AdminSurveyPushScheduleReq request
    ) {
        validateSurveyKey(surveyKey);
        validateAdminRequest(request);

        LocalDateTime dMinus3At = request.deadlineNotificationAt().minusDays(3);
        if (!request.startNotificationAt().isBefore(dMinus3At)) {
            throw new GlobalException(INVALID_INPUT);
        }

        upsertSchedule(
                surveyKey,
                SurveyNotificationStage.STARTED,
                null,
                request.startNotificationAt(),
                request.rewardPoint()
        );
        upsertSchedule(
                surveyKey,
                SurveyNotificationStage.D_MINUS_3,
                null,
                dMinus3At,
                request.rewardPoint()
        );
        upsertSchedule(
                surveyKey,
                SurveyNotificationStage.DEADLINE,
                null,
                request.deadlineNotificationAt(),
                request.rewardPoint()
        );

        return getAdminSchedules(surveyKey);
    }

    public AdminSurveyPushScheduleRes getAdminSchedules(String surveyKey) {
        validateSurveyKey(surveyKey);

        Map<SurveyNotificationStage, SurveyPushSchedule> schedules = new EnumMap<>(SurveyNotificationStage.class);
        surveyPushScheduleRepository.findAllBySurveyKeyAndNotificationStageIn(surveyKey, ADMIN_STAGES)
                .forEach(schedule -> schedules.put(schedule.getNotificationStage(), schedule));

        int rewardPoint = schedules.values()
                .stream()
                .findFirst()
                .map(SurveyPushSchedule::getRewardPoint)
                .orElse(0);

        return new AdminSurveyPushScheduleRes(
                surveyKey,
                rewardPoint,
                AdminSurveyPushScheduleStageRes.from(schedules.get(SurveyNotificationStage.STARTED)),
                AdminSurveyPushScheduleStageRes.from(schedules.get(SurveyNotificationStage.D_MINUS_3)),
                AdminSurveyPushScheduleStageRes.from(schedules.get(SurveyNotificationStage.DEADLINE))
        );
    }

    @Transactional
    public AdminSurveyPushScheduleRes cancelSchedules(String surveyKey) {
        validateSurveyKey(surveyKey);

        LocalDateTime now = LocalDateTime.now(clock);
        surveyPushScheduleRepository.findAllBySurveyKeyOrderByNotificationStageAscSurveyPushScheduleIdAsc(surveyKey)
                .stream()
                .filter(SurveyPushSchedule::isPending)
                .forEach(schedule -> schedule.cancel(now));

        return getAdminSchedules(surveyKey);
    }

    @Transactional
    public SurveyReminderRes remindAfterLater(
            String surveyKey,
            Long userId
    ) {
        validateSurveyKey(surveyKey);
        if (userId == null || userId <= 0) {
            throw new GlobalException(INVALID_INPUT);
        }

        SurveyPushSchedule deadline = surveyPushScheduleRepository
                .findBySurveyKeyAndNotificationStage(surveyKey, SurveyNotificationStage.DEADLINE)
                .orElseThrow(() -> new GlobalException(INVALID_INPUT));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime remindAt = now.plusDays(1);
        SurveyReminderSuppressedBy suppressedBy = suppressionForReminder(surveyKey, remindAt, deadline.getScheduledAt());
        if (!SurveyReminderSuppressedBy.NONE.equals(suppressedBy)) {
            return new SurveyReminderRes(surveyKey, false, null, suppressedBy);
        }

        String idempotencyKey = idempotencyKey(
                surveyKey,
                SurveyNotificationStage.REMIND_AFTER_LATER,
                userId
        );

        return surveyPushScheduleRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> updateExistingReminder(surveyKey, remindAt, deadline.getRewardPoint(), existing))
                .orElseGet(() -> createReminder(surveyKey, userId, remindAt, deadline.getRewardPoint(), idempotencyKey));
    }

    private void upsertSchedule(
            String surveyKey,
            SurveyNotificationStage stage,
            Long targetUserId,
            LocalDateTime scheduledAt,
            int rewardPoint
    ) {
        String idempotencyKey = idempotencyKey(surveyKey, stage, targetUserId);
        surveyPushScheduleRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresentOrElse(
                        schedule -> schedule.updatePendingSchedule(scheduledAt, rewardPoint),
                        () -> surveyPushScheduleRepository.save(new SurveyPushSchedule(
                                surveyKey,
                                stage,
                                targetUserId,
                                scheduledAt,
                                rewardPoint,
                                idempotencyKey,
                                LocalDateTime.now(clock)
                        ))
                );
    }

    private SurveyReminderRes updateExistingReminder(
            String surveyKey,
            LocalDateTime remindAt,
            int rewardPoint,
            SurveyPushSchedule existing
    ) {
        if (!existing.isPending()) {
            return new SurveyReminderRes(
                    surveyKey,
                    false,
                    null,
                    SurveyReminderSuppressedBy.ALREADY_PROCESSED
            );
        }

        existing.updatePendingSchedule(remindAt, rewardPoint);
        return new SurveyReminderRes(
                surveyKey,
                true,
                existing.getScheduledAt(),
                SurveyReminderSuppressedBy.NONE
        );
    }

    private SurveyReminderRes createReminder(
            String surveyKey,
            Long userId,
            LocalDateTime remindAt,
            int rewardPoint,
            String idempotencyKey
    ) {
        // 현재 서버에는 설문 참여 완료 상태를 확인할 도메인이 없어 미참여 조건은 후속 설문 기능 연동이 필요하다.
        SurveyPushSchedule saved = surveyPushScheduleRepository.save(new SurveyPushSchedule(
                surveyKey,
                SurveyNotificationStage.REMIND_AFTER_LATER,
                userId,
                remindAt,
                rewardPoint,
                idempotencyKey,
                LocalDateTime.now(clock)
        ));

        return new SurveyReminderRes(
                surveyKey,
                true,
                saved.getScheduledAt(),
                SurveyReminderSuppressedBy.NONE
        );
    }

    private SurveyReminderSuppressedBy suppressionForReminder(
            String surveyKey,
            LocalDateTime remindAt,
            LocalDateTime deadlineAt
    ) {
        if (remindAt.isAfter(deadlineAt)) {
            return SurveyReminderSuppressedBy.EXPIRED;
        }

        LocalDate remindDate = remindAt.toLocalDate();
        if (remindDate.equals(deadlineAt.toLocalDate())) {
            return SurveyReminderSuppressedBy.DEADLINE;
        }

        return surveyPushScheduleRepository
                .findBySurveyKeyAndNotificationStage(surveyKey, SurveyNotificationStage.D_MINUS_3)
                .map(SurveyPushSchedule::getScheduledAt)
                .map(LocalDateTime::toLocalDate)
                .filter(remindDate::equals)
                .map(ignored -> SurveyReminderSuppressedBy.D3)
                .orElse(SurveyReminderSuppressedBy.NONE);
    }

    private void validateAdminRequest(AdminSurveyPushScheduleReq request) {
        if (request == null
                || request.startNotificationAt() == null
                || request.deadlineNotificationAt() == null
                || request.rewardPoint() == null
                || request.rewardPoint() < 0) {
            throw new GlobalException(INVALID_INPUT);
        }

        if (!request.startNotificationAt().isBefore(request.deadlineNotificationAt())) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private void validateSurveyKey(String surveyKey) {
        if (surveyKey == null || surveyKey.isBlank() || surveyKey.length() > MAX_SURVEY_KEY_LENGTH) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    public static String idempotencyKey(
            String surveyKey,
            SurveyNotificationStage stage,
            Long targetUserId
    ) {
        if (SurveyNotificationStage.REMIND_AFTER_LATER.equals(stage)) {
            return "survey:" + surveyKey + ":" + stage.name() + ":" + targetUserId;
        }
        return "survey:" + surveyKey + ":" + stage.name();
    }
}
