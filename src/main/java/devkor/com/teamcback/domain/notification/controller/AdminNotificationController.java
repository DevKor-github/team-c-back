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

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(
            summary = "푸시 대상 installation 검색",
            description = """
                userId 또는 installationId를 기준으로
                푸시 발송 대상 기기를 조회합니다.
                ExpoPushToken 원문은 응답하지 않습니다.
                """
    )
    @GetMapping("/installations/search")
    public CommonResponse<List<AdminPushInstallationRes>> searchInstallations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String installationId
    ) {
        return CommonResponse.success(adminNotificationService.searchInstallations(userId, installationId));
    }

    @Operation(
            summary = "관리자 푸시 발송 미리보기",
            description = """
                푸시를 실제로 생성하지 않고
                대상 기기 수와 최종 payload를 확인합니다.
                PushDispatch와 PushMessage는 저장하지 않습니다.
                """
    )
    @PostMapping("/dispatches/preview")
    public CommonResponse<AdminPushDispatchPreviewRes> preview(
            @RequestBody AdminPushDispatchReq request
    ) {
        return CommonResponse.success(adminNotificationService.preview(request));
    }


    @Operation(
            summary = "관리자 푸시 수동 발송",
            description = """
                관리자가 입력한 내용으로
                PushDispatch와 PushMessage를 생성합니다.
                실제 Expo 전송은 기존 비동기 worker가 처리합니다.
                """
    )
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

    @Operation(
            summary = "관리자 푸시 발송 이력 조회",
            description = """
                관리자 푸시 발송 내역을 최신순으로 조회합니다.
                appVariant와 발송 상태로 필터링할 수 있습니다.
                """
    )

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

    @Operation(
            summary = "관리자 푸시 발송 상세 조회",
            description = """
                발송 기본 정보와 전체 대상 수,
                메시지 상태별 처리 건수를 조회합니다.
                ExpoPushToken과 개별 메시지 전체 목록은 반환하지 않습니다.
                """
    )
    @GetMapping("/dispatches/{dispatchId}")
    public CommonResponse<AdminPushDispatchDetailRes> getDispatch(
            @PathVariable Long dispatchId
    ) {
        return CommonResponse.success(adminNotificationService.getDispatch(dispatchId));
    }
}
