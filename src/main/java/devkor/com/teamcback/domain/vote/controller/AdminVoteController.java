package devkor.com.teamcback.domain.vote.controller;

import devkor.com.teamcback.domain.vote.dto.response.ChangeVoteStatusRes;
import devkor.com.teamcback.domain.vote.service.VoteService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/{voteId}")
    public CommonResponse<ChangeVoteStatusRes> changeVoteStatus(
            @Parameter(description = "투표 ID", example = "1")
            @PathVariable(name = "voteId") Long voteId) {
        return CommonResponse.success(voteService.changeVoteStatus(voteId));
    }
}
