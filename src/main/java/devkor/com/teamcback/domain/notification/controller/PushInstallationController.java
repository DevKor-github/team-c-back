package devkor.com.teamcback.domain.notification.controller;


import devkor.com.teamcback.domain.notification.dto.request.PushInstallationRegisterReq;
import devkor.com.teamcback.domain.notification.service.PushInstallationService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "푸시 알림 설치 관리",
        description = "로그인 사용자의 Expo Push installation 등록, 갱신 및 비활성화 API"
)
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/installations")
public class PushInstallationController {

    private final PushInstallationService pushInstallationService;

    @Operation(
            summary = "푸시 알림 installation 등록 및 갱신",
            description = """
                    현재 로그인 사용자의 Expo Push installation을 등록합니다.
                    동일한 installationId로 다시 요청하면 새 행을 생성하지 않고
                    기존 installation의 토큰, 사용자 및 앱 환경 정보를 갱신합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 또는 갱신 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청값",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @PutMapping("/{installationId}")
    public ResponseEntity<CommonResponse<Void>> register(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetail,

            @Parameter(
                    description = "앱 설치를 식별하는 고유 ID",
                    example = "11111111-2222-4333-8444-555555555555",
                    required = true
            )
            @PathVariable
            @NotBlank
            @Size(max = 64)
            String installationId,

            @Valid
            @RequestBody
            PushInstallationRegisterReq request
    ) {
        Long userId = userDetail.getUser().getUserId();

        pushInstallationService.register(
                userId,
                installationId,
                request.expoPushToken(),
                request.appVariant()
        );

        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(
            summary = "푸시 알림 installation 비활성화",
            description = """
                    현재 로그인 사용자가 소유한 installation을 비활성화합니다.
                    대상이 없거나 이미 비활성화된 경우에도 성공으로 처리합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @DeleteMapping("/{installationId}")
    public ResponseEntity<CommonResponse<Void>> deactivate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetail,

            @Parameter(
                    description = "비활성화할 앱 설치 식별자",
                    example = "11111111-2222-4333-8444-555555555555",
                    required = true
            )
            @PathVariable
            @NotBlank
            @Size(max = 64)
            String installationId
    ) {
        Long userId = userDetail.getUser().getUserId();

        pushInstallationService.deactivate(
                userId,
                installationId
        );

        return ResponseEntity.ok(CommonResponse.success());
    }
}
