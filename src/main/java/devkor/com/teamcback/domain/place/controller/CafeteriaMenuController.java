package devkor.com.teamcback.domain.place.controller;

import devkor.com.teamcback.domain.place.dto.response.GetCafeteriaMenuListRes;
import devkor.com.teamcback.domain.place.service.CafeteriaMenuService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places/cafeterias")
public class CafeteriaMenuController {

    private final CafeteriaMenuService cafeteriaMenuService;

    @Operation(summary = "건물 id와 날짜로 학식 메뉴 검색",
            description = "건물 id와 날짜로 학식 메뉴 검색")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/menus")
    public CommonResponse<GetCafeteriaMenuListRes> getCafeteriaMenu(
            @Parameter(name = "placeId", description = "장소 ID") @RequestParam Long placeId,
            @Parameter(name = "startDate", description = "2025-12-25 형식의 요청 시작 날짜") @RequestParam LocalDate startDate,
            @Parameter(name = "endDate", description = "2025-12-25 형식의 요청 마지막 날짜") @RequestParam LocalDate endDate) {

        return CommonResponse.success(cafeteriaMenuService.getCafeteriaMenu(placeId, startDate, endDate));
    }
}
