package devkor.com.teamcback.domain.report.controller;

import devkor.com.teamcback.domain.report.dto.request.UpdateReportStatusReq;
import devkor.com.teamcback.domain.report.dto.response.GetReportListRes;
import devkor.com.teamcback.domain.report.dto.response.UpdateReportStatusRes;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.service.ReportService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final ReportService reportService;

    @Operation(summary = "신고 관리를 위한 목록 조회",
            description = "신고 상태에 따라 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping
    public CommonResponse<GetReportListRes> getReportList(
            @Parameter(name = "reportStatus", description = "신고 상태 종류") @RequestParam(required = false) ReportStatus reportStatus
    ) {
        return CommonResponse.success(reportService.getReportList(reportStatus));
    }

    @Operation(summary = "신고 상태 변경",
            description = "신고 상태 변경")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping("/{reportId}")
    public CommonResponse<UpdateReportStatusRes> updateReportStatus(
            @Parameter(description = "변경할 신고 id") @PathVariable Long reportId,
            @Parameter(description = "신고의 변경할 생태") @RequestBody UpdateReportStatusReq req
    ) {
        return CommonResponse.success(reportService.updateReportStatus(reportId, req));
    }
}
