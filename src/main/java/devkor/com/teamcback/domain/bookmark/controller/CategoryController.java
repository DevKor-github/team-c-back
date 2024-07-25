package devkor.com.teamcback.domain.bookmark.controller;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.response.*;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 카테고리 조회
     * @param userDetail 사용자 정보
     * @param categoryId 카테고리 id
     * @return 카테고리 id, 이름, 색상, 메모
     */
    @Operation(summary = "카테고리 조회", description = "즐겨찾기 카테고리 조회: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/{categoryId}")
    public CommonResponse<GetCategoryRes> getCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId) {
        return CommonResponse.success(categoryService.getCategory(userDetail.getUser().getUserId(), categoryId));
    }

    /**
     * 카테고리 전체 조회
     * @param userDetail 사용자 정보
     * @return 카테고리 List: [{카테고리 id, 카테고리명, 색상, 메모, 각 카테고리의 즐겨찾기 개수}]
     */
    @Operation(summary = "카테고리 전체 조회", description = "모든 카테고리 목록 조회: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping
    public CommonResponse<List<GetCategoryRes>> getAllCategories(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        return CommonResponse.success(categoryService.getAllCategories(userDetail.getUser().getUserId()));
    }

    /**
     * 특정 카테고리 상세 조회 (즐겨찾기 List)
     * @param userDetail 사용자 정보
     * @param categoryId 카테고리 id
     * @return 즐겨찾기 List: [{즐겨찾기 id, 장소 타입, 장소 id, 메모}]
     */
    @Operation(summary = "카테고리 전체 조회", description = "카테고리의 모든 즐겨찾기 조회: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/{categoryId}/bookmarks")
    public CommonResponse<List<GetBookmarkRes>> getAllBookmarks(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId) {
        return CommonResponse.success(categoryService.getAllBookmarks(userDetail.getUser().getUserId(), categoryId));
    }

    /**
     * 즐겨찾기 생성
     * @param userDetail 사용자정보
     * @param categoryId 카테고리 Id
     * @param req 즐겨찾기 생성을 위한 정보
     */
    @Operation(summary = "즐겨찾기 생성", description = "특정 카테고리의 즐겨찾기 생성: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("/{categoryId}/bookmarks")
    public CommonResponse<CreateBookmarkRes> createCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모", required = true)
        @RequestBody CreateBookmarkReq req) {
        return CommonResponse.success(categoryService.createBookmark(userDetail.getUser().getUserId(), categoryId, req));
    }

    /**
     * 즐겨찾기 삭제
     * @param userDetail 사용자 정보
     * @param categoryId 삭제할 즐겨찾기가 속하는 카테고리 id
     * @param bookmarkId 삭제할 즐겨찾기 id
     */
    @Operation(summary = "즐겨찾기 삭제", description = "즐겨찾기 삭제: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "즐겨찾기를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @DeleteMapping("/{categoryId}/bookmarks/{bookmarkId}")
    public CommonResponse<DeleteBookmarkRes> deleteBookmark(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "즐겨찾기 id", required = true)
        @PathVariable Long bookmarkId) {
        return CommonResponse.success(categoryService.deleteBookmark(userDetail.getUser().getUserId(), categoryId, bookmarkId));
    }

    /**
     * 즐겨찾기 수정
     * @param userDetail 사용자 정보
     * @param categoryId 카테고리 id
     * @param bookmarkId 수정할 즐겨찾기 id
     * @param req 즐겨찾기 수정을 위한 정보
     */
    @Operation(summary = "즐겨찾기 수정", description = "즐겨찾기 수정: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "즐겨찾기를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping("/{categoryId}/bookmarks/{bookmarkId}")
    public CommonResponse<ModifyBookmarkRes> modifyBookmark(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "즐겨찾기 id", required = true)
        @PathVariable Long bookmarkId,
        @Parameter(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모", required = true)
        @RequestBody CreateBookmarkReq req) {
        return CommonResponse.success(categoryService.modifyBookmark(userDetail.getUser().getUserId(), categoryId, bookmarkId, req));
    }

    /**
     * 즐겨찾기 조회
     * @param userDetail 사용자 정보
     * @param categoryId 카테고리 id
     * @return 즐겨찾기 id, 장소 타입, 장소 id, 메모
     */
    @Operation(summary = "즐겨찾기 조회", description = "즐겨찾기 카테고리 조회: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/{categoryId}/bookmarks/{bookmarkId}")
    public CommonResponse<GetBookmarkRes> getBookmark(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "카테고리 id", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "즐겨찾기 id", required = true)
        @PathVariable Long bookmarkId) {
        return CommonResponse.success(categoryService.getBookmark(userDetail.getUser().getUserId(), categoryId, bookmarkId));
    }

}
