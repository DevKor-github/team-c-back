package devkor.com.teamcback.domain.vote.controller;

import devkor.com.teamcback.domain.vote.dto.response.ChangeVoteStatusRes;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteRes;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import devkor.com.teamcback.domain.vote.service.VoteService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.CursorPageRes;
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
@RequestMapping("/api/admin/votes")
public class AdminVoteController {
    private final VoteService voteService;

    /**
     * 투표 상태 변경
     */
    @Operation(summary = "투표 상태 변경", description = "투표 상태 변경")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PatchMapping("/{voteId}")
    public CommonResponse<ChangeVoteStatusRes> changeVoteStatus(
            @Parameter(description = "투표 ID", example = "1")
            @PathVariable(name = "voteId") Long voteId,
            @Parameter(description = "투표 상태", example = "REFLECTED")
            @RequestParam(name = "status") VoteStatus status) {
        return CommonResponse.success(voteService.changeVoteStatus(voteId, status));
    }

    /**
     * 투표 조회
     */
    @Operation(summary = "투표 조회", description = "투표 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping()
    public CommonResponse<CursorPageRes<GetVoteRes>> getVoteList(
            @Parameter(description = "투표 상태", example = "REFLECTED")
            @RequestParam(name = "status", required = false) VoteStatus status,
            @Parameter(description = "마지막 조회 투표 ID", example = "0")
            @RequestParam(name = "lastVoteId", defaultValue = "0", required = false) Long lastVoteId,
            @Parameter(description = "조회 size", example = "20")
            @RequestParam(name = "size", defaultValue = "20", required = false) int size
            ) {
        return CommonResponse.success(voteService.getVoteList(status, lastVoteId, size));
    }
}
