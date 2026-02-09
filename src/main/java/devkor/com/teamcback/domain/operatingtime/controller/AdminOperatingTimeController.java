package devkor.com.teamcback.domain.operatingtime.controller;

import devkor.com.teamcback.domain.operatingtime.dto.request.SavePlaceOperatingTimeReq;
import devkor.com.teamcback.domain.operatingtime.dto.response.GetPlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.dto.response.SavePlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.service.AdminOperatingTimeService;
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
@RequestMapping("/api/admin/operating-time")
public class AdminOperatingTimeController {
    private final AdminOperatingTimeService adminOperatingTimeService;

    @GetMapping("/places/{placeId}")
    @Operation(summary = "장소의 운영시간 검색",
            description = "장소 id로 운영시간 검색")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
      })
    public CommonResponse<GetPlaceOperatingTimeRes> getPlaceOperatingTime(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId) {
        return CommonResponse.success(adminOperatingTimeService.getPlaceOperatingTime(placeId));
    }

    @PutMapping("/places/{placeId}")
    @Operation(summary = "장소의 운영시간 저장",
            description = "장소가 각 요일별로 고정된 운영시간을 가지는 경우(00:00-00:00 형식 포함)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SavePlaceOperatingTimeRes> savePlaceOperatingTime(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
            @Parameter(description = "요일별 운영 시간") @RequestBody SavePlaceOperatingTimeReq req) {
        return CommonResponse.success(adminOperatingTimeService.savePlaceOperatingTime(placeId, req));
    }
}
