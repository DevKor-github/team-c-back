package devkor.com.teamcback.domain.admin.controller;

import devkor.com.teamcback.domain.admin.dto.request.SaveBuildingImageReq;
import devkor.com.teamcback.domain.admin.dto.response.SaveBuildingImageRes;
import devkor.com.teamcback.domain.admin.service.AdminBuildingService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/buildings")
public class AdminBuildingController {
    private final AdminBuildingService adminBuildingService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "건물 내부 사진 저장",
        description = "건물 내부 사진 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "403", description = "잘못된 입력입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SaveBuildingImageRes> createNode(
        @Parameter(description = "저장할 건물 내부 사진 정보") @RequestPart("req") SaveBuildingImageReq req,
        @Parameter(description = "저장할 사진 파일") @RequestPart("image") MultipartFile image
        ) {
        return CommonResponse.success(adminBuildingService.saveBuildingImage(req, image));
    }
}
