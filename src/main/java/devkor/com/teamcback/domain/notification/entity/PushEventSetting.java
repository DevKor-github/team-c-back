package devkor.com.teamcback.domain.notification.entity;
import devkor.com.teamcback.domain.notification.persistence.PushStore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@PushStore @Entity @Table(name="tb_push_event_setting") @Getter @NoArgsConstructor
public class PushEventSetting {
    @Id @Column(length=100) private String id;
    @Column(nullable=false) private boolean enabled;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    public PushEventSetting(String id, boolean enabled) { this.id=id; this.enabled=enabled; this.updatedAt=LocalDateTime.now(); }
}
