package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.response.UpdateNoticeListRes;
import devkor.com.teamcback.domain.notification.service.UpdateNoticeService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/notices")
public class UpdateNoticeController {

    private final UpdateNoticeService updateNoticeService;

    @GetMapping
    @Operation(
            summary = "업데이트 공지 목록 조회",
            description = "공개 시각이 지난 공지를 최신순으로 조회합니다. show=true인 공지만 앱 팝업 후보입니다."
    )
    public CommonResponse<UpdateNoticeListRes> getNotices() {
        return CommonResponse.success(updateNoticeService.getPublishedNotices());
    }
}
