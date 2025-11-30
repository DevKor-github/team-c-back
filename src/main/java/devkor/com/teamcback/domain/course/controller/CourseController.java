package devkor.com.teamcback.domain.course.controller;

import devkor.com.teamcback.domain.course.dto.response.GetCourseListRes;
import devkor.com.teamcback.domain.course.service.CourseService;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceListRes;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class CourseController {
    private final CourseService courseService;

    @GetMapping("/{placeId}/courses")
    @Operation(summary = "장소 id로 강의 리스트 검색",
            description = "강의 list 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetCourseListRes> getCourseList(@Parameter(name = "placeId", description = "장소 ID", example = "4424", required = true) @PathVariable Long placeId) {
        return CommonResponse.success(courseService.getCourseList(placeId));
    }
}
