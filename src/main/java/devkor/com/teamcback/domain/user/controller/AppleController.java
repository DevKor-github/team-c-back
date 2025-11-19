package devkor.com.teamcback.domain.user.controller;

import com.apple.itunes.storekit.verification.VerificationException;
import devkor.com.teamcback.domain.user.dto.request.AppleNotationReq;
import devkor.com.teamcback.domain.user.dto.response.AppleNotificationRes;
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
    public CommonResponse<AppleNotificationRes> handleAppleNotification(@RequestBody AppleNotationReq request) throws VerificationException {

        return CommonResponse.success(appleService.handleAppleNotification(request));

    }
}
