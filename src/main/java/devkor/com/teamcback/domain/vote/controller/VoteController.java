package devkor.com.teamcback.domain.vote.controller;

import devkor.com.teamcback.domain.vote.dto.response.GetVoteRes;
import devkor.com.teamcback.domain.vote.dto.response.SaveVoteRecordRes;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/votes")
public class VoteController {
    private final VoteService voteService;

    /**
     * 투표 리스트 조회
     * @param status 조회할 투표 상태
     */
    @Operation(summary = "투표 리스트 조회", description = "투표 리스트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("")
    public CommonResponse<List<GetVoteRes>> getVoteList(
            @Parameter(name = "status", description = "투표 상태", example = "OPEN")
            @RequestParam(name = "status", required = false) VoteStatus status) {
        return CommonResponse.success(voteService.getVoteList(status));
    }


    /**
     * 투표 정보 조회
     * @param voteTopicId 투표 주제 ID
     */
    @Operation(summary = "투표 정보 조회", description = "투표 정보 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/{voteTopicId}")
    public CommonResponse<GetVoteRes> getVote(
            @Parameter(description = "투표 주제 ID", example = "1")
            @PathVariable(name = "voteTopicId") Long voteTopicId) {
        return CommonResponse.success(voteService.getVote(voteTopicId));
    }

    /**
     * 투표 기록 저장
     * @param userDetail 사용자 정보
     * @param voteTopicId 투표 주제 ID
     * @param voteOptionId 투표 옵션 ID
     */
    @Operation(summary = "투표 저장", description = "투표 저장")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("/{voteTopicId}/records")
    public CommonResponse<SaveVoteRecordRes> saveVoteRecord(
            @Parameter(description = "사용자정보")
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(name = "voteTopicId", description = "투표 주제 ID", example = "1")
            @PathVariable(name = "voteTopicId") Long voteTopicId,
            @Parameter(name = "voteOptionId", description = "투표 옵션 ID", example = "1")
            @RequestParam(name = "voteOptionId") Long voteOptionId,
            @Parameter(name = "placeId", description = "투표 장소 ID", example = "1")
            @RequestParam(name = "placeId", required = false) Long placeId
            ) {
        Long userId = userDetail == null ? null : userDetail.getUser().getUserId();
        return CommonResponse.success(voteService.saveVoteRecord(userId, voteTopicId, voteOptionId, placeId));
    }

}
