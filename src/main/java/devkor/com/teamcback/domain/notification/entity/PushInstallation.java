package devkor.com.teamcback.domain.notification.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_push_installation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_push_installation_installation_id",
                        columnNames = "installation_id"
                ),
                @UniqueConstraint(
                        name = "uk_push_installation_expo_push_token",
                        columnNames = "expo_push_token"
                )
        },
        indexes = {
                @Index(
                        name = "idx_push_installation_user_variant_active",
                        columnList = "user_id, app_variant, active"
                )
        }
)
@NoArgsConstructor
@Getter
public class PushInstallation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_installation_id")
    private Long pushInstallationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "installation_id", nullable = false, length = 64)
    private String installationId;

    @Column(name = "expo_push_token", nullable = false, length = 255)
    private String expoPushToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_variant", nullable = false, length = 20)
    private AppVariant appVariant;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    public PushInstallation(
            Long userId,
            String installationId,
            String expoPushToken,
            AppVariant appVariant
    ) {
        this.userId = userId;
        this.installationId = installationId;
        this.expoPushToken = expoPushToken;
        this.appVariant = appVariant;
        this.active = true;
    }

    public void register(
            Long userId,
            String installationId,
            String expoPushToken,
            AppVariant appVariant
    ) {
        this.userId = userId;
        this.installationId = installationId;
        this.expoPushToken = expoPushToken;
        this.appVariant = appVariant;
        this.active = true;
        this.deactivatedAt = null;
    }

    public void deactivate(LocalDateTime now) {
        if (!active) {
            return;
        }

        this.active = false;
        this.deactivatedAt = now;
    }
}
