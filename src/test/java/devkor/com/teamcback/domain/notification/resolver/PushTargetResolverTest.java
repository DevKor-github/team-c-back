package devkor.com.teamcback.domain.notification.resolver;

import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushTargetResolverTest {

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    private PushTargetResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PushTargetResolver(pushInstallationRepository);
    }

    @Test
    void allTargetResolvesDistinctActiveProductionInstallations() {
        PushInstallation first = new PushInstallation(1L, "install-1", "ExponentPushToken[first]", AppVariant.PRODUCTION);
        PushInstallation duplicate = new PushInstallation(2L, "install-1", "ExponentPushToken[duplicate]", AppVariant.PRODUCTION);
        PushInstallation second = new PushInstallation(3L, "install-2", "ExponentPushToken[second]", AppVariant.PRODUCTION);
        when(pushInstallationRepository.findAllByAppVariantAndActiveTrue(AppVariant.PRODUCTION))
                .thenReturn(List.of(first, duplicate, second));

        List<PushInstallation> resolved = resolver.resolve(PushTargetType.ALL, "ALL", AppVariant.PRODUCTION);

        assertThat(resolved).containsExactly(first, second);
    }

    @Test
    void allTargetRejectsNonAllTargetValue() {
        assertThatThrownBy(() -> resolver.resolve(PushTargetType.ALL, "1", AppVariant.PRODUCTION))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void previewAllTargetAllowsZeroActiveInstallations() {
        when(pushInstallationRepository.findAllByAppVariantAndActiveTrue(AppVariant.PRODUCTION))
                .thenReturn(List.of());

        List<PushInstallation> resolved = resolver.resolveForPreview(
                PushTargetType.ALL,
                "ALL",
                AppVariant.PRODUCTION
        );

        assertThat(resolved).isEmpty();
    }

    @Test
    void actualAllTargetStillRejectsZeroActiveInstallations() {
        when(pushInstallationRepository.findAllByAppVariantAndActiveTrue(AppVariant.PRODUCTION))
                .thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve(PushTargetType.ALL, "ALL", AppVariant.PRODUCTION))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void selectedTargetsResolveDistinctActiveInstallations() {
        PushInstallation first = new PushInstallation(1L, "install-1", "ExponentPushToken[first]", AppVariant.DEV);
        PushInstallation second = new PushInstallation(2L, "install-2", "ExponentPushToken[second]", AppVariant.DEV);
        when(pushInstallationRepository.findByInstallationIdAndAppVariantAndActiveTrue("install-1", AppVariant.DEV))
                .thenReturn(Optional.of(first));
        when(pushInstallationRepository.findByInstallationIdAndAppVariantAndActiveTrue("install-2", AppVariant.DEV))
                .thenReturn(Optional.of(second));

        List<PushInstallation> resolved = resolver.resolveSelected(
                List.of("install-1", "install-2", "install-1"),
                AppVariant.DEV
        );

        assertThat(resolved).containsExactly(first, second);
    }

    @Test
    void selectedTargetsRejectWhenNoInstallationRemainsActive() {
        when(pushInstallationRepository.findByInstallationIdAndAppVariantAndActiveTrue("install-1", AppVariant.DEV))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveSelected(List.of("install-1"), AppVariant.DEV))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void selectedTargetsRejectBlankInstallationId() {
        assertThatThrownBy(() -> resolver.resolveSelected(List.of("install-1", " "), AppVariant.DEV))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }
}
