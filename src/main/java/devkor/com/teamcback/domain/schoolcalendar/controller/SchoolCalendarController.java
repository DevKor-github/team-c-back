package devkor.com.teamcback.domain.schoolcalendar.controller;

import devkor.com.teamcback.domain.schoolcalendar.dto.response.GetSchoolCalendarRes;
import devkor.com.teamcback.domain.schoolcalendar.service.SchoolCalendarService;
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
@RequestMapping("/api/school-calendar")
public class SchoolCalendarController {
    private final SchoolCalendarService schoolCalendarService;

    /***
     * 방학 여부 반환
     */
    @GetMapping("/vacation")
    @Operation(summary = "방학 여부를 t/f로 반환", description = "방학 여부를 t/f로 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetSchoolCalendarRes> isVacation() {
        return CommonResponse.success(schoolCalendarService.isVacation());
    }

    /***
     * 고연전 여부 반환
     */
    @GetMapping("/koyeon")
    @Operation(summary = "고연전 여부를 t/f로 반환", description = "고연전 여부를 t/f로 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetSchoolCalendarRes> isKoyeon() {
        return CommonResponse.success(schoolCalendarService.isKoyeon());
    }

}
