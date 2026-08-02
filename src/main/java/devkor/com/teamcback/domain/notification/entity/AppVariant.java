package devkor.com.teamcback.domain.notification.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum AppVariant {

    DEV,
    PREVIEW,
    PRODUCTION;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AppVariant from(String value) {
        if (value == null) {
            return null;
        }

        return AppVariant.valueOf(
                value.trim().toUpperCase(Locale.ROOT)
        );
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
