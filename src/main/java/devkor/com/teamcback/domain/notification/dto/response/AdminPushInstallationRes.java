package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import java.time.LocalDateTime;

public record AdminPushInstallationRes(
        Long userId,
        String installationId,
        AppVariant appVariant,
        boolean active,
        LocalDateTime lastActiveAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deactivatedAt,
        String username,
        String email
) {

    public AdminPushInstallationRes(PushInstallation installation) {
        this(installation, null, null);
    }

    public AdminPushInstallationRes(PushInstallation installation, String username, String email) {
        this(
                installation.getUserId(),
                installation.getInstallationId(),
                installation.getAppVariant(),
                installation.isActive(),
                installation.isActive()
                        ? installation.getModifiedAt()
                        : installation.getDeactivatedAt(),
                installation.getCreatedAt(),
                installation.getModifiedAt(),
                installation.getDeactivatedAt(),
                username,
                email
        );
    }
}
