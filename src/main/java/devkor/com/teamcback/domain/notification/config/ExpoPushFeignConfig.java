package devkor.com.teamcback.domain.notification.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public class ExpoPushFeignConfig {

    @Bean
    public Request.Options expoPushRequestOptions(ExpoPushProperties properties) {
        return new Request.Options(
                properties.connectTimeout().toMillis(),
                TimeUnit.MILLISECONDS,
                properties.readTimeout().toMillis(),
                TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public RequestInterceptor expoPushAuthorizationInterceptor(ExpoPushProperties properties) {
        return template -> {
            if (StringUtils.hasText(properties.accessToken())) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken());
            }
        };
    }

    @Bean
    public Retryer expoPushRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
