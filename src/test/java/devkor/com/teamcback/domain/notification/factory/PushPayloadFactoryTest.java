package devkor.com.teamcback.domain.notification.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.validation.PushActionValidator;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushPayloadFactoryTest {

    private final PushPayloadFactory factory = new PushPayloadFactory(
            new PushActionValidator(),
            new ObjectMapper()
    );

    @Test
    void includesTargetAppVariantInDeepLinkPayload() {
        PushPayload payload = factory.create(
                "notification-1",
                "title",
                "body",
                PushMode.ACTUAL,
                AppVariant.PREVIEW,
                PushActionType.HOME,
                Map.of()
        );

        assertThat(payload.data().appVariant()).isEqualTo("preview");
        assertThat(payload.data().action().type()).isEqualTo("HOME");
        assertThat(payload.image()).isNull();
    }

    @Test
    void includesNormalizedImageUrlInPayload() {
        PushPayload payload = factory.create(
                "notification-1",
                "title",
                "body",
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                PushActionType.HOME,
                Map.of(),
                "  https://cdn.kodaero.store/push/building.png  "
        );

        assertThat(payload.image()).isEqualTo("https://cdn.kodaero.store/push/building.png");
    }

    @Test
    void rejectsNonHttpsImageUrl() {
        assertThatThrownBy(() -> factory.create(
                "notification-1",
                "title",
                "body",
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                PushActionType.HOME,
                Map.of(),
                "http://cdn.kodaero.store/push/building.png"
        ))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void blankImageUrlIsTreatedAsAbsent() {
        PushPayload payload = factory.create(
                "notification-1",
                "title",
                "body",
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                PushActionType.HOME,
                Map.of(),
                "   "
        );

        assertThat(payload.image()).isNull();
    }
}
