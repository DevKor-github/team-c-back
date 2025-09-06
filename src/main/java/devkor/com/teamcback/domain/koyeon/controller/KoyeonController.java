package devkor.com.teamcback.domain.koyeon.controller;

import devkor.com.teamcback.domain.koyeon.dto.response.GlobalPubSearchListRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubInfoRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubListRes;
import devkor.com.teamcback.domain.koyeon.entity.Koyeon;
import devkor.com.teamcback.domain.koyeon.service.KoyeonService;
import devkor.com.teamcback.global.response.CommonResponse;
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
@RequestMapping("/api/koyeon")
public class KoyeonController {
    private final KoyeonService koyeonService;

    /***
     * 고연전 여부 반환
     * TODO: 프론트 api 변경 후 삭제 필요
     */
    @GetMapping("")
    @Operation(summary = "고연전 시즌 여부를 t/f로 반환", description = "고연전 시즌 여부를 t/f로 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<Koyeon> isKoyeon() {
        return CommonResponse.success(koyeonService.isKoyeon());
    }

    /***
     * 주점 통합 검색
     * @param keyword
     */
    @GetMapping("/search")
    @Operation(summary = "주점 통합 검색", description = "주점 통합 검색")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GlobalPubSearchListRes> globalPubSearch(
        @Parameter(name = "keyword", description = "검색어", example = "치킨")
        @RequestParam String keyword
    ) {
        return CommonResponse.success(koyeonService.globalPubSearch(keyword));
    }

    /***
     * 주점 List 반환
     */
    @GetMapping("/pubs")
    @Operation(summary = "주점 List 반환", description = "주점 List 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchFreePubListRes> searchFreePubList(
        @Parameter(name = "tagId", description = "태그 id", example = "1")
        @RequestParam(required = false) Long tagId
    ) {
        return CommonResponse.success(koyeonService.searchFreePubList(tagId));
    }

    /***
     * 특정 주점의 정보 반환
     * @param pubId 주점 id
     */
    @GetMapping("/pubs/{pubId}")
    @Operation(summary = "특정 주점 정보 반환", description = "특정 주점 정보 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchFreePubInfoRes> searchFreePubInfo(
        @Parameter(name = "pubId", description = "주점 id", example = "1", required = true)
        @PathVariable Long pubId){
        return CommonResponse.success(koyeonService.searchFreePubInfo(pubId));
    }

}
