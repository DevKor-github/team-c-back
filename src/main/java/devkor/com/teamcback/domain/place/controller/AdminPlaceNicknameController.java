package devkor.com.teamcback.domain.place.controller;

import devkor.com.teamcback.domain.place.dto.request.SavePlaceNicknameReq;
import devkor.com.teamcback.domain.place.dto.response.DeletePlaceNicknameRes;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceNicknameListRes;
import devkor.com.teamcback.domain.place.dto.response.SavePlaceNicknameRes;
import devkor.com.teamcback.domain.place.dto.response.UpdatePlaceNicknamesRes;
import devkor.com.teamcback.domain.place.service.AdminPlaceNicknameService;
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
@RequestMapping("/api/admin/places")
public class AdminPlaceNicknameController {
    private final AdminPlaceNicknameService adminPlaceNicknameService;

    @PostMapping("/{placeId}/nicknames")
    @Operation(summary = "장소 별명 저장",
        description = "장소 별명 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SavePlaceNicknameRes> saveClassroomNickname(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
        @Parameter(description = "장소 별명 저장 dto") @RequestBody SavePlaceNicknameReq req) {
        return CommonResponse.success(adminPlaceNicknameService.saveClassroomNickname(placeId, req));
    }

    @DeleteMapping("/nicknames/{nicknameId}")
    @Operation(summary = "장소 별명 삭제",
        description = "장소 별명 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소 별명을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeletePlaceNicknameRes> deleteClassroomNickname(
        @Parameter(name = "nicknameId", description = "장소 별명 ID") @PathVariable Long nicknameId) {
        return CommonResponse.success(adminPlaceNicknameService.deleteClassroomNickname(nicknameId));
    }

    @GetMapping("/{placeId}/nicknames")
    @Operation(summary = "장소 별명 조회",
        description = "장소 별명 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetPlaceNicknameListRes> getClassroomNickname(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId) {
        return CommonResponse.success(adminPlaceNicknameService.getClassroomNickname(placeId));
    }

    /***
     * Place Nickname Tables 업데이트
     */
    @PostMapping("/nicknames")
    @Operation(summary = "Place Nickname Tables 업데이트", description = "Place Nickname Tables 업데이트")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<UpdatePlaceNicknamesRes> updatePlaceNicknames() {
        return CommonResponse.success(adminPlaceNicknameService.updatePlaceNicknames());
    }
}
