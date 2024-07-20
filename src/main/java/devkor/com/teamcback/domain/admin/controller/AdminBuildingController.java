package devkor.com.teamcback.domain.admin.controller;

import devkor.com.teamcback.domain.admin.dto.response.DeleteBuildingImageRes;
import devkor.com.teamcback.domain.admin.dto.response.ModifyBuildingImageRes;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/buildings")
public class AdminBuildingController {
    private final AdminBuildingService adminBuildingService;

    @PostMapping(value = "/image",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public CommonResponse<SaveBuildingImageRes> saveBuildingImage(
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor,
        @Parameter(description = "저장할 사진 파일") @RequestPart("image") MultipartFile image
        ) {
        return CommonResponse.success(adminBuildingService.saveBuildingImage(buildingId, floor, image));
    }

    @PutMapping(value = "/image/{buildingImageId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "건물 내부 사진 수정",
        description = "건물 내부 사진 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "403", description = "잘못된 입력입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyBuildingImageRes> modifyBuildingImage(
        @Parameter(description = "수정할 건물 사진 ID") @PathVariable Long buildingImageId,
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor,
        @Parameter(description = "수정할 사진 파일") @RequestPart("image") MultipartFile image
    ) {
        return CommonResponse.success(adminBuildingService.modifyBuildingImage(buildingImageId, buildingId, floor, image));
    }

    @DeleteMapping(value = "/image/{buildingImageId}")
    @Operation(summary = "건물 내부 사진 삭제",
        description = "건물 내부 사진 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeleteBuildingImageRes> deleteBuildingImage(
        @Parameter(description = "삭제할 건물 사진 ID") @PathVariable Long buildingImageId
    ) {
        return CommonResponse.success(adminBuildingService.deleteBuildingImage(buildingImageId));
    }
}
