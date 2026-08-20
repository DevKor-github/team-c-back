package devkor.com.teamcback.domain.notification.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.expo")
public record ExpoPushProperties(
        String baseUrl,
        String accessToken,
        Duration connectTimeout,
        Duration readTimeout
) {

    public ExpoPushProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://exp.host/--/api/v2/push";
        }
        if (accessToken == null) {
            accessToken = "";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
