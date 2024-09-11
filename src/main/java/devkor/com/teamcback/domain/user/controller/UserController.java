package devkor.com.teamcback.domain.user.controller;

import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.dto.response.ModifyUsernameRes;
import devkor.com.teamcback.domain.user.dto.response.LoginUserRes;
import devkor.com.teamcback.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    /**
     * 마이페이지 정보 조회
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "마이페이지 정보 조회", description = "마이페이지 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/mypage")
    public CommonResponse<GetUserInfoRes> getUserInfo(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        return CommonResponse.success(userService.getUserInfo(userDetail.getUser().getUserId()));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public CommonResponse<LoginUserRes> login(
        @RequestBody LoginUserReq loginUserReq
    ) {
        return CommonResponse.success(userService.login(loginUserReq));
    }

    /**
     * 사용자 별명 수정
     * @param userDetail 사용자 정보
     * @param username 새로운 사용자명
     */
    @Operation(summary = "사용자 별명 수정", description = "사용자 별명 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping()
    public CommonResponse<ModifyUsernameRes> modifyUsername(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "사용자명", required = true)
        @RequestParam String username) {
        return CommonResponse.success(userService.modifyUsername(userDetail.getUser().getUserId(), username));
    }
}
