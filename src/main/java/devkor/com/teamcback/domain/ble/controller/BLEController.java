package devkor.com.teamcback.domain.ble.controller;

import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
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
    public CommonResponse<UpdateBLERes> updateBLE(
            @Valid @RequestBody UpdateBLEReq updateBLEReq){
        return CommonResponse.success(bleService.updateBLE(updateBLEReq));
    }
}
