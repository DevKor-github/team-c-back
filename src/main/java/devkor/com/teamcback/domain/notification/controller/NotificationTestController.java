package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.request.NotificationTestReq;
import devkor.com.teamcback.domain.notification.dto.response.NotificationTestRes;
import devkor.com.teamcback.domain.notification.service.NotificationTestService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationTestController {

    private final NotificationTestService notificationTestService;

    /**
     * DEV/PREVIEW 환경에서 실제 기기 푸시 수신을 확인하는 API입니다.
     * 테스트 코드가 아닌 애플리케이션 기능입니다.
     */
    @Operation(
            summary = "푸시 알림 테스트 발송",
            description = """
                    인증된 사용자의 본인 DEV/PREVIEW installation 한 대에
                    실제 테스트 푸시를 발송합니다.
                    PRODUCTION installation은 사용할 수 없습니다.
                    """
    )
    @PostMapping("/test")
    public ResponseEntity<CommonResponse<NotificationTestRes>> sendTest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetailsImpl userDetail,

            @Parameter(
                    description = "중복 발송 방지를 위한 멱등성 키",
                    required = true,
                    example = "7b347ad7-6138-4cb7-af7d-f5201703a596"
            )
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,

            @Valid @RequestBody NotificationTestReq request
    ) {
        if (userDetail == null) {
            throw new GlobalException(UNAUTHORIZED);
        }

        Long userId = userDetail.getUser().getUserId();

        return ResponseEntity.ok(CommonResponse.success(
                notificationTestService.sendTest(
                        userId,
                        idempotencyKey,
                        request
                )
        ));
    }
}
