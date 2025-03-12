package devkor.com.teamcback.domain.suggestion.controller;

import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.dto.response.CreateSuggestionRes;
import devkor.com.teamcback.domain.suggestion.service.SuggestionService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/suggestions")
public class SuggestionController {
    private final SuggestionService suggestionService;

    /**
     * 건의 작성
     * @param req 건의 작성을 위한 정보
     */
    @Operation(summary = "건의 작성", description = "건의 작성")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "401", description = "권한이 없는 사용자입니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "입력이 잘못되었습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping
    public CommonResponse<CreateSuggestionRes> createSuggestion(
            @Parameter(description = "사용자정보")
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(description = "건의 제목, 분류, 내용, 이메일", required = true)
            @RequestPart(value = "req") CreateSuggestionReq req,
            @Parameter(description = "건의 사진") @RequestPart(value = "images", required = false) List<MultipartFile> images
            ) {
        Long userId = userDetail == null ? null : userDetail.getUser().getUserId();
        return CommonResponse.success(suggestionService.createSuggestion(userId, req, images));
    }
}
