package devkor.com.teamcback.domain.suggestion.controller;

import devkor.com.teamcback.domain.suggestion.dto.response.GetSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.ModifySuggestionRes;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import devkor.com.teamcback.domain.suggestion.service.SuggestionService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/suggestions")
public class AdminSuggestionController {
    private final SuggestionService suggestionService;
    private final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_SIZE = "20";
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_IS_ASC = "false";


    /**
     * 건의 조회
     */
    @Operation(summary = "건의 조회", description = "분류에 해당하는 건의 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping
    public CommonResponse<Page<GetSuggestionRes>> getSuggestions(
        @Parameter(description = "조회 페이지, 없으면 1")
        @RequestParam(value = "page", defaultValue = DEFAULT_PAGE) int page,
        @Parameter(description = "페이지 내 건의 개수, 없으면 20")
        @RequestParam(value = "size", defaultValue = DEFAULT_SIZE) int size,
        @Parameter(description = "정렬 기준, 없으면 생성일")
        @RequestParam(value = "sortBy", defaultValue = DEFAULT_SORT_BY) String sortBy,
        @Parameter(description = "정렬 순서, 없으면 내림차순")
        @RequestParam(value = "isAsc", defaultValue = DEFAULT_IS_ASC) boolean isAsc,
        @Parameter(description = "조회할 건의 종류, 없으면 전체")
        @RequestParam(value = "type", required = false) SuggestionType type,
        @Parameter(description = "조회할 건의의 해결 여부, 없으면 전체")
        @RequestParam(value = "isSolved", required = false) Boolean isSolved
        ) {
        return CommonResponse.success(suggestionService.getSuggestions(page-1, size, sortBy, isAsc, type, isSolved));
    }

    /**
     * 건의 해결 여부 수정
     */
    @Operation(summary = "건의 수정", description = "건의 해결 여부 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "건의를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping("/{suggestionId}")
    public CommonResponse<ModifySuggestionRes> modifySuggestions(
        @Parameter(description = "건의 ID")
        @PathVariable Long suggestionId,
        @Parameter(description = "해결 여부")
        @RequestParam(value = "isSolved") boolean isSolved
    ) {
        return CommonResponse.success(suggestionService.modifySuggestions(suggestionId, isSolved));
    }
}
