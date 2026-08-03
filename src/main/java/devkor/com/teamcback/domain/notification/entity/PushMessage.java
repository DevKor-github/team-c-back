package devkor.com.teamcback.domain.notification.entity;

import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tb_push_message",
        indexes = {
                @Index(
                        name = "idx_push_message_dispatch",
                        columnList = "push_dispatch_id"
                ),
                @Index(
                        name = "idx_push_message_status_next_retry_at",
                        columnList = "status, next_retry_at"
                )
        }
)
@NoArgsConstructor
@Getter
public class PushMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_message_id")
    private Long pushMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "push_dispatch_id", nullable = false)
    private PushDispatch dispatch;

    @Column(name = "push_installation_id", nullable = false)
    private Long pushInstallationId;

    @Column(name = "installation_id", nullable = false, length = 64)
    private String installationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PushMessageStatus status;

    @Column(name = "expo_ticket_id", length = 255)
    private String expoTicketId;

    @Column(name = "ticket_status", length = 40)
    private String ticketStatus;

    @Column(name = "ticket_error", length = 1024)
    private String ticketError;

    @Column(name = "receipt_status", length = 40)
    private String receiptStatus;

    @Column(name = "receipt_error", length = 1024)
    private String receiptError;

    @Column(name = "send_attempts", nullable = false)
    private int sendAttempts;

    @Column(name = "receipt_attempts", nullable = false)
    private int receiptAttempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "receipt_checked_at")
    private LocalDateTime receiptCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PushMessage(
            PushDispatch dispatch,
            PushInstallation installation,
            LocalDateTime now
    ) {
        this.dispatch = dispatch;
        this.pushInstallationId = installation.getPushInstallationId();
        this.installationId = installation.getInstallationId();
        this.status = PushMessageStatus.QUEUED;
        this.expoTicketId = null;
        this.ticketStatus = null;
        this.ticketError = null;
        this.receiptStatus = null;
        this.receiptError = null;
        this.sendAttempts = 0;
        this.receiptAttempts = 0;
        this.nextRetryAt = null;
        this.sentAt = null;
        this.receiptCheckedAt = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordTicket(
            String ticketStatus,
            String expoTicketId,
            String ticketError,
            LocalDateTime now
    ) {
        this.ticketStatus = ticketStatus;
        this.expoTicketId = expoTicketId;
        this.ticketError = ticketError;
        this.sendAttempts += 1;
        this.sentAt = now;
        this.updatedAt = now;
        this.status = "ok".equals(ticketStatus)
                ? PushMessageStatus.RECEIPT_PENDING
                : PushMessageStatus.FAILED;
    }

    public void recordClientError(
            boolean retryable,
            LocalDateTime now
    ) {
        this.ticketStatus = "client_error";
        this.ticketError = retryable ? "retryable" : "non_retryable";
        this.sendAttempts += 1;
        this.updatedAt = now;
        this.status = PushMessageStatus.FAILED;
    }
}
