package devkor.com.teamcback.domain.bookmark.controller;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.response.CreateCategoryRes;
import devkor.com.teamcback.domain.bookmark.dto.response.DeleteCategoryRes;
import devkor.com.teamcback.domain.bookmark.dto.response.ModifyCategoryRes;
import devkor.com.teamcback.domain.bookmark.service.CategoryService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * 카테고리 생성
     * @param userDetail 사용자 정보
     * @param req 카테고리 생성을 위한 정보
     */
    @Operation(summary = "카테고리 생성", description = "즐겨찾기 카테고리 생성: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping
    public CommonResponse<CreateCategoryRes> createCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 이름, 색상, 메모", required = true)
        @RequestBody CreateCategoryReq req) {
        return CommonResponse.success(categoryService.createCategory(userDetail.getUser().getUserId(), req));
    }

    /**
     * 카테고리 삭제
     * @param userDetail 사용자 정보
     * @param categoryId 삭제할 카테고리 id
     */
    @Operation(summary = "카테고리 삭제", description = "즐겨찾기 카테고리 삭제: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @DeleteMapping("/{categoryId}")
    public CommonResponse<DeleteCategoryRes> deleteCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId) {
        return CommonResponse.success(categoryService.deleteCategory(userDetail.getUser().getUserId(), categoryId));
    }

    /**
     * 카테고리 수정
     * @param userDetail 사용자 정보
     * @param categoryId 수정할 카테고리 id
     * @param req 카테고리 수정을 위한 정보
     */
    @Operation(summary = "카테고리 수정", description = "즐겨찾기 카테고리 수정: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping("/{categoryId}")
    public CommonResponse<ModifyCategoryRes> modifyCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "카테고리 이름, 색상, 메모", required = true)
        @RequestBody CreateCategoryReq req) {
        return CommonResponse.success(categoryService.modifyCategory(userDetail.getUser().getUserId(), categoryId, req));
    }
}
