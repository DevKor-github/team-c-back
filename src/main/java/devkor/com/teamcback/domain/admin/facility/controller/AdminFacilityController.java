package devkor.com.teamcback.domain.admin.facility.controller;

import devkor.com.teamcback.domain.admin.facility.dto.request.CreateFacilityReq;
import devkor.com.teamcback.domain.admin.facility.dto.request.ModifyFacilityReq;
import devkor.com.teamcback.domain.admin.facility.dto.response.CreateFacilityRes;
import devkor.com.teamcback.domain.admin.facility.dto.response.DeleteFacilityRes;
import devkor.com.teamcback.domain.admin.facility.dto.response.GetFacilityListRes;
import devkor.com.teamcback.domain.admin.facility.dto.response.ModifyFacilityRes;
import devkor.com.teamcback.domain.admin.facility.service.AdminFacilityService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/facilities")
public class AdminFacilityController {
    private final AdminFacilityService adminFacilityService;

    @GetMapping
    @Operation(summary = "건물 id와 층으로 편의시설 리스트 검색",
        description = "편의시설 list 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetFacilityListRes> getClassroomList(
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor) {
        return CommonResponse.success(adminFacilityService.getFacilityList(buildingId, floor));
    }

    @PostMapping
    @Operation(summary = "편의시설 생성",
        description = "편의시설 생성")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<CreateFacilityRes> createClassroom(
        @Parameter(description = "교실 생성 요청 dto") @Valid @RequestBody CreateFacilityReq req) {
        return CommonResponse.success(adminFacilityService.createFacility(req));
    }

    @PutMapping("/{facilityId}")
    @Operation(summary = "편의시설 수정",
        description = "편의시설 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyFacilityRes> modifyClassroom(
        @Parameter(description = "수정할 편의시설 ID") @PathVariable Long facilityId,
        @Parameter(description = "편의시설 수정 요청 dto") @Valid @RequestBody ModifyFacilityReq req) {
        return CommonResponse.success(adminFacilityService.modifyFacility(facilityId, req));
    }

    @DeleteMapping("/{facilityId}")
    @Operation(summary = "편의시설 삭제",
        description = "편의시설 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "편의시설을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeleteFacilityRes> deleteNode(
        @Parameter(description = "삭제할 편의시설 ID") @PathVariable Long facilityId) {
        return CommonResponse.success(adminFacilityService.deleteFacility(facilityId));
    }
}
