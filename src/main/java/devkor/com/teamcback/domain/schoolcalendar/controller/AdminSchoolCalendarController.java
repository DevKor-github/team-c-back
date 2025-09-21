package devkor.com.teamcback.domain.schoolcalendar.controller;

import devkor.com.teamcback.domain.schoolcalendar.dto.response.UpdateSchoolCalendarRes;
import devkor.com.teamcback.domain.schoolcalendar.service.SchoolCalendarService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/school-calendar")
public class AdminSchoolCalendarController {
    private final SchoolCalendarService schoolCalendarService;

    /***
     * 방학 여부 수정
     */
    @PutMapping("/vacation")
    @Operation(summary = "방학 여부 수정(토글)", description = "방학 여부 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<UpdateSchoolCalendarRes> updateVacationActive() {
        return CommonResponse.success(schoolCalendarService.updateVacationActive());
    }

    /***
     * 고연전 여부 수정
     */
    @PutMapping("/koyeon")
    @Operation(summary = "고연전 여부 수정(토글)", description = "고연전 여부 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<UpdateSchoolCalendarRes> updateKoyeonActive() {
        return CommonResponse.success(schoolCalendarService.updateKoyeonActive());
    }

}
