package devkor.com.teamcback.domain.notification.entity;

import devkor.com.teamcback.domain.notification.persistence.PushStore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@PushStore @Entity @Table(name="tb_push_event_setting_change") @Getter @NoArgsConstructor
public class PushEventSettingChange {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="setting_id",nullable=false,length=100) private String settingId;
    @Column(nullable=false) private boolean enabled;
    @Column(name="changed_by",nullable=false,length=255) private String changedBy;
    @Column(name="changed_at",nullable=false) private LocalDateTime changedAt;
    public PushEventSettingChange(String settingId, boolean enabled, String actor) {
        this.settingId=settingId; this.enabled=enabled; this.changedBy=actor; this.changedAt=LocalDateTime.now();
    }
}
