package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.response.AdminPushEventFlagRes;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushEventFlagService {

    private final StringRedisTemplate redisTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    private devkor.com.teamcback.domain.notification.repository.PushEventSettingRepository settings;
    @Value("${push.storage:mysql}") private String storage = "mysql";
    @Value("${push.audience:INTERNAL_TEST}") private String audience = "INTERNAL_TEST";

    @org.springframework.beans.factory.annotation.Autowired
    private devkor.com.teamcback.domain.notification.repository.PushInstallationRepository installations;
    public boolean isEligible(Long userId) {
        return !"neon".equals(storage) || !"INTERNAL_TEST".equals(audience)
                || (userId != null && installations.findAdminUserIds(List.of(userId)).contains(userId));
    }
    @org.springframework.beans.factory.annotation.Autowired
    private devkor.com.teamcback.domain.notification.repository.PushEventSettingChangeRepository changes;
    public String audience() { return audience; }


    @Value("${push.event.crowd-enabled:false}")
    private boolean crowdDefaultEnabled;

    @Value("${push.event.report-enabled:false}")
    private boolean reportDefaultEnabled;

    @Value("${push.event.character-enabled:false}")
    private boolean characterDefaultEnabled;

    @Value("${push.event.survey-enabled:false}")
    private boolean surveyDefaultEnabled;

    @Value("${push.event.target-app-variants:PRODUCTION}")
    private String targetAppVariants;

    public List<AppVariant> getTargetAppVariants() {
        List<AppVariant> configuredVariants = Arrays.stream(targetAppVariants.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> AppVariant.valueOf(value.toUpperCase(Locale.ROOT)))
                .distinct()
                .toList();
        return configuredVariants.isEmpty()
                ? List.of(AppVariant.PRODUCTION)
                : configuredVariants;
    }

    public boolean isEnabled(PushEventType eventType) {
        if ("neon".equals(storage)) {
            return settings.findById(audience + ":" + eventType.name())
                    .map(devkor.com.teamcback.domain.notification.entity.PushEventSetting::isEnabled).orElse(false);
        }
        String redisValue = getRedisValue(eventType);
        if ("true".equalsIgnoreCase(redisValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(redisValue)) {
            return false;
        }
        return defaultEnabled(eventType);
    }

    public List<AdminPushEventFlagRes> getFlags() {
        return Arrays.stream(PushEventType.values())
                .map(eventType -> new AdminPushEventFlagRes(eventType, isEnabled(eventType)))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional("pushTransactionManager")
    public AdminPushEventFlagRes updateFlag(
            PushEventType eventType,
            boolean enabled
    ) {
        if ("neon".equals(storage)) {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            changes.save(new devkor.com.teamcback.domain.notification.entity.PushEventSettingChange(
                    audience + ":" + eventType.name(), enabled, authentication == null ? "SYSTEM" : authentication.getName()));
            settings.save(new devkor.com.teamcback.domain.notification.entity.PushEventSetting(audience + ":" + eventType.name(), enabled));
        } else {
            redisTemplate.opsForValue().set(eventType.redisKey(), Boolean.toString(enabled));
        }
        return new AdminPushEventFlagRes(eventType, isEnabled(eventType));
    }

    private String getRedisValue(PushEventType eventType) {
        try {
            return redisTemplate.opsForValue().get(eventType.redisKey());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean defaultEnabled(PushEventType eventType) {
        return switch (eventType) {
            case CROWD -> crowdDefaultEnabled;
            case REPORT -> reportDefaultEnabled;
            case CHARACTER -> characterDefaultEnabled;
            case SURVEY -> surveyDefaultEnabled;
        };
    }
}
