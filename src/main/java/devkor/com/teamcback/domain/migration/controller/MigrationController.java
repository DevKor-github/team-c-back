package devkor.com.teamcback.domain.migration.controller;

import devkor.com.teamcback.domain.migration.entity.Migration;
import devkor.com.teamcback.domain.migration.service.MigrationService;
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
@RequestMapping("/api/migration")
public class MigrationController {
    private final MigrationService migrationService;

    /***
     * 이전중 여부 반환
     */
    @GetMapping("")
    @Operation(summary = "스토어 이동 공지를 위한 t/f값 반환", description = "스토어 이동 공지를 위한 t/f값 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<Migration> isMigrating() {
        return CommonResponse.success(migrationService.isMigrating());
    }
}