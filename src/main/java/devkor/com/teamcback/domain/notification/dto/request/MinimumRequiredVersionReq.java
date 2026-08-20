package devkor.com.teamcback.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MinimumRequiredVersionReq(
        @NotBlank
        @Pattern(regexp = "\\d+\\.\\d+\\.\\d+")
        String minimumRequiredVersion
) {
}
