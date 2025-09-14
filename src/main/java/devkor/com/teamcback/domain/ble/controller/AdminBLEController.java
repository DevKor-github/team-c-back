package devkor.com.teamcback.domain.ble.controller;

import devkor.com.teamcback.domain.ble.dto.request.CreateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.CreateBLERes;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.service.AdminBLEService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ble")
public class AdminBLEController {
    private final AdminBLEService adminBLEService;

    @PostMapping
    @Operation(summary = "BLE장비 생성",
            description = "BLE장비 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장비를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<CreateBLERes> createBLE(
            @Parameter(description = "BLE장비 생성 요청 dto") @Valid @RequestBody CreateBLEReq createBLEReq) {
        return CommonResponse.success(adminBLEService.CreateBLEDevice(createBLEReq));
    }
}
