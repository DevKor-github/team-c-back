package devkor.com.teamcback.domain.admin.building.controller;

import devkor.com.teamcback.domain.admin.building.dto.request.SaveBuildingNicknameReq;
import devkor.com.teamcback.domain.admin.building.dto.response.SaveBuildingNicknameRes;
import devkor.com.teamcback.domain.admin.building.service.AdminBuildingNicknameService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/buildings/nickname")
public class AdminBuildingNicknameController {
    private final AdminBuildingNicknameService adminBuildingNicknameService;

    @PostMapping("/{buildingId}")
    @Operation(summary = "건물 별명 저장",
        description = "건물 별명 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SaveBuildingNicknameRes> saveBuildingNickname(
        @Parameter(name = "buildingId", description = "건물 ID") @PathVariable Long buildingId,
        @Parameter(description = "건물 별명 저장 dto") @RequestBody SaveBuildingNicknameReq req) {
        return CommonResponse.success(adminBuildingNicknameService.saveBuildingNickname(buildingId, req));
    }
}
