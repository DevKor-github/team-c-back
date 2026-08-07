package devkor.com.teamcback.domain.notification.entity;

import devkor.com.teamcback.domain.notification.entity.type.SurveyNotificationStage;
import devkor.com.teamcback.domain.notification.entity.type.SurveyPushScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tb_survey_push_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_push_schedule_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_survey_push_schedule_status_scheduled_at",
                        columnList = "status, scheduled_at"
                )
        }
)
@NoArgsConstructor
@Getter
public class SurveyPushSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_push_schedule_id")
    private Long surveyPushScheduleId;

    @Column(name = "survey_key", nullable = false, length = 64)
    private String surveyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_stage", nullable = false, length = 40)
    private SurveyNotificationStage notificationStage;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "reward_point", nullable = false)
    private int rewardPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SurveyPushScheduleStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public SurveyPushSchedule(
            String surveyKey,
            SurveyNotificationStage notificationStage,
            Long targetUserId,
            LocalDateTime scheduledAt,
            int rewardPoint,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {
        this.surveyKey = surveyKey;
        this.notificationStage = notificationStage;
        this.targetUserId = targetUserId;
        this.scheduledAt = scheduledAt;
        this.rewardPoint = rewardPoint;
        this.status = SurveyPushScheduleStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.processedAt = null;
    }

    public boolean isPending() {
        return SurveyPushScheduleStatus.PENDING.equals(status);
    }

    public void updatePendingSchedule(
            LocalDateTime scheduledAt,
            int rewardPoint
    ) {
        if (!isPending()) {
            return;
        }
        this.scheduledAt = scheduledAt;
        this.rewardPoint = rewardPoint;
    }

    public void complete(LocalDateTime now) {
        this.status = SurveyPushScheduleStatus.COMPLETED;
        this.processedAt = now;
    }

    public void cancel(LocalDateTime now) {
        this.status = SurveyPushScheduleStatus.CANCELLED;
        this.processedAt = now;
    }

    public void skip(LocalDateTime now) {
        this.status = SurveyPushScheduleStatus.SKIPPED;
        this.processedAt = now;
    }
}
