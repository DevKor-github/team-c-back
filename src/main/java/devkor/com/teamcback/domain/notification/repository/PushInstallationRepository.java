package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushInstallationRepository extends JpaRepository<PushInstallation, Long> {

    Optional<PushInstallation> findByInstallationId(
            String installationId
    );

    Optional<PushInstallation> findByExpoPushToken(
            String expoPushToken
    );

    Optional<PushInstallation> findByInstallationIdAndUserId(
            String installationId,
            Long userId
    );

    List<PushInstallation> findAllByUserIdAndActiveTrue(
            Long userId
    );

    Optional<PushInstallation> findByInstallationIdAndAppVariantAndActiveTrue(
            String installationId,
            AppVariant appVariant
    );

    List<PushInstallation> findAllByUserIdAndAppVariantAndActiveTrue(
            Long userId,
            AppVariant appVariant
    );
}
