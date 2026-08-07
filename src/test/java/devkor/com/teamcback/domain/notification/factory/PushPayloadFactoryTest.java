package devkor.com.teamcback.domain.notification.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.validation.PushActionValidator;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    }
}
