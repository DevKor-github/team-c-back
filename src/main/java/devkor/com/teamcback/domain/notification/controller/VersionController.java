package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.service.VersionService;
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
@RequestMapping("/api/notifications/version")
public class VersionController {

    private final VersionService versionService;

    /***
     * 앱 버전 조회
     */
    @GetMapping("")
    @Operation(
            summary = "필수 업데이트 최소 버전 조회",
            description = "설치 버전이 응답 버전보다 낮은 경우 필수 업데이트 안내를 표시합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<String> getVersion() {
        return CommonResponse.success(versionService.getMinimumRequiredVersion());
    }
}
