package devkor.com.teamcback.domain.notification.entity;

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
        name = "tb_push_dispatch",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_push_dispatch_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_push_dispatch_status_created_at",
                        columnList = "status, created_at"
                )
        }
)
@NoArgsConstructor
@Getter
public class PushDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_dispatch_id")
    private Long pushDispatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private PushMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_variant", nullable = false, length = 20)
    private AppVariant appVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PushTargetType targetType;

    @Column(name = "target_value", nullable = false, length = 128)
    private String targetValue;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1024)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private PushActionType actionType;

    @Column(name = "action_params", nullable = false, length = 2048)
    private String actionParams;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PushDispatchStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public PushDispatch(
            NotificationType notificationType,
            PushMode mode,
            AppVariant appVariant,
            PushTargetType targetType,
            String targetValue,
            String title,
            String body,
            PushActionType actionType,
            String actionParams,
            String idempotencyKey,
            Long createdBy,
            LocalDateTime createdAt
    ) {
        this.notificationType = notificationType;
        this.mode = mode;
        this.appVariant = appVariant;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.title = title;
        this.body = body;
        this.actionType = actionType;
        this.actionParams = actionParams;
        this.recipientCount = 0;
        this.status = PushDispatchStatus.QUEUED;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.completedAt = null;
    }

    public void updateRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }
}
