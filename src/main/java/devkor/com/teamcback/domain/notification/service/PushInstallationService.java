package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushInstallationService {

    private final PushInstallationRepository repository;
    private final Clock clock;

    @Transactional
    public void register(
            Long userId,
            String installationId,
            String expoPushToken,
            AppVariant appVariant
    ) {
        Optional<PushInstallation> installationMatch = repository.findByInstallationId(installationId);

        Optional<PushInstallation> tokenMatch = repository.findByExpoPushToken(expoPushToken);

        if (installationMatch.isEmpty() && tokenMatch.isEmpty()) {
            repository.save(
                    new PushInstallation(
                            userId,
                            installationId,
                            expoPushToken,
                            appVariant
                    )
            );
            return;
        }

        if (installationMatch.isPresent() && tokenMatch.isEmpty()) {
            installationMatch.get().register(
                    userId,
                    installationId,
                    expoPushToken,
                    appVariant
            );
            return;
        }

        if (installationMatch.isEmpty()) {
            tokenMatch.get().register(
                    userId,
                    installationId,
                    expoPushToken,
                    appVariant
            );
            return;
        }

        PushInstallation installationEntity = installationMatch.get();

        PushInstallation tokenEntity = tokenMatch.get();

        if (installationEntity == tokenEntity
                || installationEntity.getPushInstallationId()
                .equals(tokenEntity.getPushInstallationId())) {

            installationEntity.register(
                    userId,
                    installationId,
                    expoPushToken,
                    appVariant
            );
            return;
        }

        repository.delete(tokenEntity);
        repository.flush();

        installationEntity.register(
                userId,
                installationId,
                expoPushToken,
                appVariant
        );
    }

    @Transactional
    public void deactivate(
            Long userId,
            String installationId
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        repository.findByInstallationIdAndUserId(
                        installationId,
                        userId
                )
                .ifPresent(installation ->
                        installation.deactivate(now)
                );
    }

    @Transactional
    public void deactivateAll(
            Long userId
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        repository.findAllByUserIdAndActiveTrue(userId)
                .forEach(installation ->
                        installation.deactivate(now)
                );
    }
}
