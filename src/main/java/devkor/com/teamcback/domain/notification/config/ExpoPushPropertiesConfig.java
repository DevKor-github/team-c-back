package devkor.com.teamcback.domain.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ExpoPushProperties.class,
        PushWorkerProperties.class,
        PushReceiptWorkerProperties.class,
        PushRecoveryWorkerProperties.class
})
public class ExpoPushPropertiesConfig {
}
