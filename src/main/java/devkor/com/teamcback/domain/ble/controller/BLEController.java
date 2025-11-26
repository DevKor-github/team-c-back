package devkor.com.teamcback.domain.ble.controller;

import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.BLETimePatternRes;
import devkor.com.teamcback.domain.ble.dto.response.GetBLERes;
import devkor.com.teamcback.domain.ble.dto.response.UpdateBLERes;
import devkor.com.teamcback.domain.ble.service.BLEService;
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
@RequestMapping("/api/ble")

public class BLEController {
    private final BLEService bleService;

    /***
     *
     * @param placeId BLE정보를 얻고자 하는 place Id
     */
    @GetMapping
    @Operation(summary = "placeId를 통한 BLE device 상태 요청",
            description = "placeId를 통한 BLE device 상태 요청")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetBLERes> getBLE(
            @Parameter(name="placeId", description = "BLE 정보를 얻고자 하는 placeId")
            @RequestParam Long placeId) {
        return CommonResponse.success(bleService.getBLE(placeId));
    }

    @PutMapping
    @Operation(summary = "ble 기기에서 전송하는 데이터 통한 데이터 축적",
            description = "ble 기기에서 전송하는 데이터 통한 데이터 축적")
    public CommonResponse<UpdateBLERes> updateBLE(
            @Valid @RequestBody UpdateBLEReq updateBLEReq){
        return CommonResponse.success(bleService.updateBLE(updateBLEReq));
    }

    @GetMapping("/pattern")
    @Operation(summary = "BLE 요일/시간대별 평균 인원 조회",
            description = "최근 1달 동안 특정 placeId에 대해 요일별 7/10/13/16/19/22시 기준 평균 인원 수를 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소 또는 장비를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<BLETimePatternRes> getBLETimePattern(
            @Parameter(name="placeId", description = "BLE 정보를 얻고자 하는 placeId")
            @RequestParam Long placeId) {
        return CommonResponse.success(bleService.getBLETimePattern(placeId));
    }

}
