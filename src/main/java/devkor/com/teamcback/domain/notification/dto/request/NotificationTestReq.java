package devkor.com.teamcback.domain.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationTestReq(
        @NotNull
        @Min(1)
        @Max(1)
        Integer schemaVersion,

        @NotBlank
        @Size(max = 64)
        String installationId
) {
}
