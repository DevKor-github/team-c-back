package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.response.AdminPushEventFlagRes;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushEventFlagServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PushEventFlagService service;

    @BeforeEach
    void setUp() {
        service = new PushEventFlagService(redisTemplate);
        ReflectionTestUtils.setField(service, "crowdDefaultEnabled", false);
        ReflectionTestUtils.setField(service, "reportDefaultEnabled", true);
        ReflectionTestUtils.setField(service, "characterDefaultEnabled", false);
        ReflectionTestUtils.setField(service, "surveyDefaultEnabled", false);
        ReflectionTestUtils.setField(service, "targetAppVariants", "DEV,PREVIEW");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsConfiguredDevelopmentTargetVariants() {
        service.isEnabled(PushEventType.CROWD);

        assertThat(service.getTargetAppVariants())
                .containsExactly(AppVariant.DEV, AppVariant.PREVIEW);
    }

    @Test
    void returnsYamlDefaultWhenRedisValueDoesNotExist() {
        when(valueOperations.get(PushEventType.CROWD.redisKey())).thenReturn(null);
        when(valueOperations.get(PushEventType.REPORT.redisKey())).thenReturn(null);
        when(valueOperations.get(PushEventType.SURVEY.redisKey())).thenReturn(null);

        assertThat(service.isEnabled(PushEventType.CROWD)).isFalse();
        assertThat(service.isEnabled(PushEventType.REPORT)).isTrue();
        assertThat(service.isEnabled(PushEventType.SURVEY)).isFalse();
    }

    @Test
    void redisTrueOrFalseOverridesYamlDefault() {
        when(valueOperations.get(PushEventType.CROWD.redisKey())).thenReturn("true");
        when(valueOperations.get(PushEventType.REPORT.redisKey())).thenReturn("false");

        assertThat(service.isEnabled(PushEventType.CROWD)).isTrue();
        assertThat(service.isEnabled(PushEventType.REPORT)).isFalse();
    }

    @Test
    void returnsYamlDefaultWhenRedisReadFails() {
        when(valueOperations.get(PushEventType.REPORT.redisKey())).thenThrow(new RuntimeException("redis down"));

        assertThat(service.isEnabled(PushEventType.REPORT)).isTrue();
    }

    @Test
    void updatedValueIsReflectedImmediatelyInQueryResult() {
        when(valueOperations.get(PushEventType.CHARACTER.redisKey()))
                .thenReturn(null)
                .thenReturn("true")
                .thenReturn("true");

        assertThat(service.isEnabled(PushEventType.CHARACTER)).isFalse();

        AdminPushEventFlagRes updated = service.updateFlag(PushEventType.CHARACTER, true);
        assertThat(updated.enabled()).isTrue();

        List<AdminPushEventFlagRes> flags = service.getFlags();
        assertThat(flags)
                .filteredOn(flag -> flag.eventType() == PushEventType.CHARACTER)
                .singleElement()
                .extracting(AdminPushEventFlagRes::enabled)
                .isEqualTo(true);
    }
}
