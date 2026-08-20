package devkor.com.teamcback.domain.notification.dto.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushRequest(
        String to,
        String title,
        String body,
        String sound,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> richContent,
        Object data
) {

    public ExpoPushRequest(
            String to,
            String title,
            String body,
            String sound,
            Object data
    ) {
        this(to, title, body, sound, null, data);
    }

    public static Map<String, String> richContentForImage(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : Map.of("image", imageUrl);
    }
}
