package devkor.com.teamcback.domain.user.controller;

import devkor.com.teamcback.domain.user.dto.request.AppleNotationReq;
import devkor.com.teamcback.domain.user.service.AppleService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apple")
public class AppleController {

    private final AppleService appleService;

    /**
     * Apple 로그인 계정 상태 알림을 수신하는 엔드포인트
     */
    @PostMapping("/notifications")
    public CommonResponse<> handleAppleNotification(@RequestBody AppleNotationReq request) {

        String signedPayload = request.getSignedPayload();

        if (signedPayload == null || signedPayload.isEmpty()) {
            return new GlobalException(ResultCode.INVALID_INPUT);
        }

        try {
            // 1. JWS 검증 및 디코딩 후 비즈니스 로직 수행
            notificationService.processSignedPayload(signedPayload);

            // 2. Apple 서버에 성공적인 수신을 알리기 위해 반드시 200 OK를 반환해야 합니다.
            return new CommonResponse.success();
        } catch (Exception e) {
            System.err.println("Apple SiWA Notification processing failed: " + e.getMessage());
            return new GlobalException(INTERNAL_SERVER_ERROR);
        }
    }
}
