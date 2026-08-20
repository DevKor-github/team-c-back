package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.request.AdminUpdateNoticeReq;
import devkor.com.teamcback.domain.notification.dto.response.AdminUpdateNoticeRes;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import devkor.com.teamcback.domain.notification.service.AdminUpdateNoticeService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications/notices")
public class AdminUpdateNoticeController {

    private final AdminUpdateNoticeService adminUpdateNoticeService;

    @GetMapping
    @Operation(summary = "관리자 공지 목록 조회")
    public CommonResponse<Page<AdminUpdateNoticeRes>> getNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UpdateNoticeStatus status,
            @RequestParam(required = false) String query
    ) {
        return CommonResponse.success(adminUpdateNoticeService.getNotices(page, size, status, query));
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "관리자 공지 상세 조회")
    public CommonResponse<AdminUpdateNoticeRes> getNotice(@PathVariable Long noticeId) {
        return CommonResponse.success(adminUpdateNoticeService.getNotice(noticeId));
    }

    @PostMapping
    @Operation(summary = "관리자 공지 등록", description = "공지만 저장하며 푸시는 발송하지 않습니다.")
    public CommonResponse<AdminUpdateNoticeRes> createNotice(
            @Valid @RequestBody AdminUpdateNoticeReq request
    ) {
        return CommonResponse.success(adminUpdateNoticeService.createNotice(request));
    }

    @PutMapping("/{noticeId}")
    @Operation(summary = "관리자 공지 수정", description = "공지 데이터만 수정하며 푸시는 발송하지 않습니다.")
    public CommonResponse<AdminUpdateNoticeRes> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody AdminUpdateNoticeReq request
    ) {
        return CommonResponse.success(adminUpdateNoticeService.updateNotice(noticeId, request));
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "관리자 공지 보관", description = "공지 데이터를 삭제하지 않고 보관 상태로 전환합니다.")
    public CommonResponse<AdminUpdateNoticeRes> archiveNotice(@PathVariable Long noticeId) {
        return CommonResponse.success(adminUpdateNoticeService.archiveNotice(noticeId));
    }
}
