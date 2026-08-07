package devkor.com.teamcback.domain.notification.dto.request;

import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Expo Push installation 등록 요청")
public record PushInstallationRegisterReq(

        @Schema(description = "요청 스키마 버전", example = "1", allowableValues = {"1"})
        @NotNull
        @Min(1)
        @Max(1)
        Integer schemaVersion,

        @Schema(description = "Expo에서 발급된 Push Token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        @NotBlank
        String expoPushToken,

        @Schema(description = "토큰이 발급된 앱 빌드 환경", example = "dev",
                allowableValues = {
                        "dev",
                        "preview",
                        "production"
                }
        )
        @NotNull
        AppVariant appVariant
) {
}
