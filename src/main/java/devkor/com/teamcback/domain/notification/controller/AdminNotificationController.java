package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.request.AdminPushDispatchReq;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchDetailRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchPreviewRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchSummaryRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushInstallationRes;
import devkor.com.teamcback.domain.notification.dto.response.PushDispatchEnqueueRes;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.service.AdminNotificationService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private static final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_SIZE = "20";

    private final AdminNotificationService adminNotificationService;

    @GetMapping("/installations/search")
    public CommonResponse<List<AdminPushInstallationRes>> searchInstallations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String installationId
    ) {
        return CommonResponse.success(adminNotificationService.searchInstallations(userId, installationId));
    }

    @PostMapping("/dispatches/preview")
    public CommonResponse<AdminPushDispatchPreviewRes> preview(
            @RequestBody AdminPushDispatchReq request
    ) {
        return CommonResponse.success(adminNotificationService.preview(request));
    }

    @PostMapping("/dispatches")
    public CommonResponse<PushDispatchEnqueueRes> enqueue(
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AdminPushDispatchReq request
    ) {
        if (userDetail == null) {
            throw new GlobalException(UNAUTHORIZED);
        }

        return CommonResponse.success(adminNotificationService.enqueue(
                userDetail.getUser().getUserId(),
                idempotencyKey,
                request
        ));
    }

    @GetMapping("/dispatches")
    public CommonResponse<Page<AdminPushDispatchSummaryRes>> getDispatches(
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(required = false) AppVariant appVariant,
            @RequestParam(required = false) PushDispatchStatus status
    ) {
        return CommonResponse.success(adminNotificationService.getDispatches(
                page,
                size,
                appVariant,
                status
        ));
    }

    @GetMapping("/dispatches/{dispatchId}")
    public CommonResponse<AdminPushDispatchDetailRes> getDispatch(
            @PathVariable Long dispatchId
    ) {
        return CommonResponse.success(adminNotificationService.getDispatch(dispatchId));
    }
}
