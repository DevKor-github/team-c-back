package devkor.com.teamcback.domain.notification.dto.request;

import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUpdateNoticeReq(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String description,
        @Size(max = 20) List<@NotBlank @Size(max = 500) String> features,
        @NotNull LocalDateTime publishedAt,
        @NotBlank @Size(max = 40) String appVersion,
        @NotNull Boolean show,
        @Size(max = 1000) String linkUrl,
        @Size(max = 100) String linkLabel,
        @NotNull UpdateNoticeStatus status
) {
}
