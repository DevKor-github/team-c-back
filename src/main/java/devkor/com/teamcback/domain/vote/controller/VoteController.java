package devkor.com.teamcback.domain.vote.controller;

import devkor.com.teamcback.domain.vote.dto.request.SaveVoteRecordReq;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteRes;
import devkor.com.teamcback.domain.vote.dto.response.SaveVoteRecordRes;
import devkor.com.teamcback.domain.vote.service.VoteService;
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
@RequestMapping("/api/votes")
public class VoteController {
    private final VoteService voteService;

    /**
     * 장소별 투표 정보 조회
     * @param placeId 장소 ID
     */
    @Operation(summary = "투표 정보 조회", description = "투표 정보 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/topics/{voteTopicId}/places/{placeId}")
    public CommonResponse<GetVoteRes> getVoteByPlace(
            @Parameter(description = "투표 주제 ID", example = "1")
            @PathVariable(name = "voteTopicId") Long voteTopicId,
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable(name = "placeId") Long placeId) {
        return CommonResponse.success(voteService.getVoteByPlace(voteTopicId, placeId));
    }

    /**
     * 투표 기록 저장
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "투표 저장", description = "투표 저장")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("")
    public CommonResponse<SaveVoteRecordRes> saveVoteRecord(
            @Parameter(description = "사용자정보")
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(description = "투표 저장")
            @RequestBody SaveVoteRecordReq req
            ) {
        Long userId = userDetail == null ? null : userDetail.getUser().getUserId();
        return CommonResponse.success(voteService.saveVoteRecord(userId, req));
    }

}
