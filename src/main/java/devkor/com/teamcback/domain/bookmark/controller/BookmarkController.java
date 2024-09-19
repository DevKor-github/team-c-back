package devkor.com.teamcback.domain.bookmark.controller;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.response.CreateBookmarkRes;
import devkor.com.teamcback.domain.bookmark.dto.response.DeleteBookmarkRes;
import devkor.com.teamcback.domain.bookmark.dto.response.ModifyBookmarkRes;
import devkor.com.teamcback.domain.bookmark.service.BookmarkService;
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
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 즐겨찾기 생성 or 수정
     * @param userDetail 사용자정보
     * @param req 즐겨찾기 생성을 위한 정보
     */
    @Operation(summary = "즐겨찾기 생성 또는 수정", description = "즐겨찾기 생성 또는 수정: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping
    public CommonResponse<CreateBookmarkRes> createCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모", required = true)
        @RequestBody CreateBookmarkReq req) {
        return CommonResponse.success(bookmarkService.createBookmark(userDetail.getUser().getUserId(), req));
    }

    /**
     * 즐겨찾기 삭제
     * @param userDetail 사용자 정보
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
    @DeleteMapping("/{bookmarkId}")
    public CommonResponse<DeleteBookmarkRes> deleteBookmark(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "즐겨찾기 id", required = true)
        @PathVariable Long bookmarkId) {
        return CommonResponse.success(bookmarkService.deleteBookmark(userDetail.getUser().getUserId(), bookmarkId));
    }

    /**
     * 즐겨찾기 수정
     * @param userDetail 사용자 정보
     * @param bookmarkId 수정할 즐겨찾기 id
     * @param req 즐겨찾기 메모 수정을 위한 정보
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
    @PutMapping("/{bookmarkId}")
    public CommonResponse<ModifyBookmarkRes> modifyBookmark(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "즐겨찾기 id", required = true)
        @PathVariable Long bookmarkId,
        @Parameter(description = "수정된 메모", required = true)
        @RequestBody ModifyBookmarkReq req) {
        return CommonResponse.success(bookmarkService.modifyBookmark(userDetail.getUser().getUserId(), bookmarkId, req));
    }
}
