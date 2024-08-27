package devkor.com.teamcback.domain.place.controller;

import devkor.com.teamcback.domain.place.dto.request.CreatePlaceReq;
import devkor.com.teamcback.domain.place.dto.request.ModifyPlaceReq;
import devkor.com.teamcback.domain.place.dto.response.CreatePlaceRes;
import devkor.com.teamcback.domain.place.dto.response.DeletePlaceRes;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceListRes;
import devkor.com.teamcback.domain.place.dto.response.ModifyPlaceRes;
import devkor.com.teamcback.domain.place.service.AdminPlaceService;
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
@RequestMapping("/api/admin/places")
public class AdminPlaceController {
    private final AdminPlaceService adminPlaceService;

    @GetMapping
    @Operation(summary = "건물 id와 층으로 장소 리스트 검색",
        description = "장소 list 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetPlaceListRes> getPlaceList(
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor) {
        return CommonResponse.success(adminPlaceService.getPlaceList(buildingId, floor));
    }

    @PostMapping
    @Operation(summary = "장소 생성",
        description = "장소 생성")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<CreatePlaceRes> createPlace(
        @Parameter(description = "교실 생성 요청 dto") @Valid @RequestBody CreatePlaceReq req) {
        return CommonResponse.success(adminPlaceService.createPlace(req));
    }

    @PutMapping("/{placeId}")
    @Operation(summary = "장소 수정",
        description = "장소 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyPlaceRes> modifyPlace(
        @Parameter(description = "수정할 장소 ID") @PathVariable Long placeId,
        @Parameter(description = "장소 수정 요청 dto") @Valid @RequestBody ModifyPlaceReq req) {
        return CommonResponse.success(adminPlaceService.modifyPlace(placeId, req));
    }

    @DeleteMapping("/{placeId}")
    @Operation(summary = "장소 삭제",
        description = "장소 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "편의시설을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeletePlaceRes> deletePlace(
        @Parameter(description = "삭제할 편의시설 ID") @PathVariable Long placeId) {
        return CommonResponse.success(adminPlaceService.deletePlace(placeId));
    }
}
