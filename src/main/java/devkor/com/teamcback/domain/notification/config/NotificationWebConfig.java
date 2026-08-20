package devkor.com.teamcback.domain.notification.config;

import devkor.com.teamcback.domain.notification.converter.AppVariantConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class NotificationWebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new AppVariantConverter());
    }
}
