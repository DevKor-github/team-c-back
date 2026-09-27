package devkor.com.teamcback.domain.notification.outbox;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="tb_push_domain_event", indexes=@Index(name="idx_push_domain_event_due",columnList="audience,completed_at,next_attempt_at"))
@Getter @NoArgsConstructor
public class PushDomainEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="event_id") private Long id;
    @Column(nullable=false,length=32) private String type;
    @Column(nullable=false,length=24) private String audience;
    @Column(nullable=false,columnDefinition="text") private String payload;
    @Column(nullable=false) private int attempts;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="next_attempt_at",nullable=false) private LocalDateTime nextAttemptAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
    public PushDomainEvent(String type,String audience,String payload,LocalDateTime now) {
        this.type=type;this.audience=audience;this.payload=payload;this.createdAt=now;this.nextAttemptAt=now;
    }
    public void complete(LocalDateTime now) { completedAt=now; }
    public void retry(LocalDateTime now) { attempts++;nextAttemptAt=now.plusSeconds(Math.min(3600,30L << Math.min(attempts,7))); }
}
