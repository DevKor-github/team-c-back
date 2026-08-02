package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static devkor.com.teamcback.global.response.ResultCode.UNSUPPORTED_REQUEST;

@Component
@RequiredArgsConstructor
public class PushTargetResolver {

    private final PushInstallationRepository pushInstallationRepository;

    public List<PushInstallation> resolve(
            PushTargetType targetType,
            String targetValue,
            AppVariant appVariant
    ) {
        if (targetType == null || targetValue == null || targetValue.isBlank() || appVariant == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        List<PushInstallation> installations = switch (targetType) {
            case INSTALLATION -> resolveInstallation(targetValue, appVariant);
            case USER -> resolveUser(targetValue, appVariant);
            case USER_GROUP -> throw new GlobalException(UNSUPPORTED_REQUEST);
        };

        List<PushInstallation> distinctInstallations = distinctByInstallation(installations);

        if (distinctInstallations.isEmpty()) {
            throw new GlobalException(INVALID_INPUT);
        }

        return distinctInstallations;
    }

    private List<PushInstallation> resolveInstallation(
            String installationId,
            AppVariant appVariant
    ) {
        return pushInstallationRepository.findByInstallationIdAndAppVariantAndActiveTrue(
                        installationId,
                        appVariant
                )
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<PushInstallation> resolveUser(
            String targetValue,
            AppVariant appVariant
    ) {
        Long userId = parsePositiveLong(targetValue);

        return pushInstallationRepository.findAllByUserIdAndAppVariantAndActiveTrue(
                userId,
                appVariant
        );
    }

    private List<PushInstallation> distinctByInstallation(List<PushInstallation> installations) {
        Map<String, PushInstallation> distinct = new LinkedHashMap<>();

        installations.forEach(installation ->
                distinct.putIfAbsent(
                        installation.getInstallationId(),
                        installation
                )
        );

        return distinct.values().stream().toList();
    }

    private Long parsePositiveLong(String value) {
        try {
            Long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new GlobalException(INVALID_INPUT);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new GlobalException(INVALID_INPUT);
        }
    }
}
