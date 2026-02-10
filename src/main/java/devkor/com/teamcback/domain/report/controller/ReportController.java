package devkor.com.teamcback.domain.report.controller;

import devkor.com.teamcback.domain.report.dto.request.CreateReviewReportReq;
import devkor.com.teamcback.domain.report.dto.response.CreateReviewReportRes;
import devkor.com.teamcback.domain.report.dto.response.GetUserReviewReportStatusRes;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.report.service.ReportService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 작성",
            description = "리뷰에 대한 신고를 작성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping(value = "/reviews/{reviewId}")
    public CommonResponse<CreateReviewReportRes> createReviewReport(
            @Parameter(description = "사용자정보") @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(name = "reviewId", description = "리뷰 ID") @PathVariable Long reviewId,
            @Parameter(description = "리뷰 작성 내용", required = true) @Valid @RequestBody CreateReviewReportReq req) {

        return CommonResponse.success(reportService.createReviewReport(userDetail == null ? null : userDetail.getUser().getUserId(), reviewId, req));
    }

    @Operation(summary = "사용자 리뷰 신고 여부 조회",
            description = "사용자 신고된 상태인지 확인하고 알림(비회원은 조회 x)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping(value = "/status")
    public CommonResponse<GetUserReviewReportStatusRes> searchUserReviewReportStatus(
            @Parameter(description = "사용자정보", required = true) @AuthenticationPrincipal UserDetailsImpl userDetail)
    {
        return CommonResponse.success(reportService.searchUserReviewReportStatus(userDetail.getUser().getUserId()));
    }
}
