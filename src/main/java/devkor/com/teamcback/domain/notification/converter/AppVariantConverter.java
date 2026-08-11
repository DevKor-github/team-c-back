package devkor.com.teamcback.domain.notification.converter;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import org.springframework.core.convert.converter.Converter;

public class AppVariantConverter implements Converter<String, AppVariant> {

    @Override
    public AppVariant convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        return AppVariant.from(source);
    }
}
