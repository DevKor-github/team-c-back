package devkor.com.teamcback.domain.koyeon.controller;

import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubInfoListRes;
import devkor.com.teamcback.domain.koyeon.entity.Koyeon;
import devkor.com.teamcback.domain.koyeon.service.KoyeonService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/koyeon")
public class KoyeonController {
    private final KoyeonService koyeonService;

    /***
     * 고연전 여부 반환
     */
    @GetMapping("")
    @Operation(summary = "고연전 시즌 여부를 t/f로 반환", description = "고연전 시즌 여부를 t/f로 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<Koyeon> isKoyeon() {
        return CommonResponse.success(koyeonService.isKoyeon());
    }

    /***
     * 고연전 여부 반환
     */
    @GetMapping("/pubs")
    @Operation(summary = "무료주점 정보 반환", description = "무료주점 정보 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchFreePubInfoListRes> getFreePubInfo() {
        return CommonResponse.success(koyeonService.searchFreePubInfo());
    }

}
