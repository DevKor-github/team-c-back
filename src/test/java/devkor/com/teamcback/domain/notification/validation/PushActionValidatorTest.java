package devkor.com.teamcback.domain.notification.validation;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushActionValidatorTest {

    private final PushActionValidator validator = new PushActionValidator();

    @Test
    void characterStoreAllowsNoParams() {
        Map<String, Object> params = validator.validateAndNormalize(
                PushActionType.CHARACTER_STORE,
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                Map.of()
        );

        assertThat(params).isEmpty();
    }

    @Test
    void characterStoreRejectsParams() {
        assertThatThrownBy(() -> validator.validateAndNormalize(
                PushActionType.CHARACTER_STORE,
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                Map.of("characterId", 1L)
        )).isInstanceOf(GlobalException.class);
    }
}
