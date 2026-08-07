package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.request.MinimumRequiredVersionReq;
import devkor.com.teamcback.domain.notification.dto.response.MinimumRequiredVersionRes;
import devkor.com.teamcback.domain.notification.service.VersionService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications/required-version")
public class AdminRequiredVersionController {

    private final VersionService versionService;

    @GetMapping
    @Operation(summary = "필수 업데이트 최소 버전 조회")
    public CommonResponse<MinimumRequiredVersionRes> getMinimumRequiredVersion() {
        return CommonResponse.success(new MinimumRequiredVersionRes(
                versionService.getMinimumRequiredVersion()
        ));
    }

    @PutMapping
    @Operation(
            summary = "필수 업데이트 최소 버전 변경",
            description = "설치 버전이 지정한 버전보다 낮은 앱만 필수 업데이트 대상으로 처리합니다."
    )
    public CommonResponse<MinimumRequiredVersionRes> updateMinimumRequiredVersion(
            @Valid @RequestBody MinimumRequiredVersionReq request
    ) {
        return CommonResponse.success(new MinimumRequiredVersionRes(
                versionService.updateMinimumRequiredVersion(request.minimumRequiredVersion())
        ));
    }
}
