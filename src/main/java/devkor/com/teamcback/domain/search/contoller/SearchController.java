package devkor.com.teamcback.domain.search.contoller;

import devkor.com.teamcback.domain.facility.entity.FacilityType;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.GetSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SaveSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="search", description = "검색기능 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    /***
     * keyword가 포함된 장소 검색
     * @param buildingId 건물 ID
     * @param keyword 검색 단어
     *
     */
    @GetMapping()
    @Operation(summary = "입력 통한 후보 검색어 조회", description = "keyword가 포함된 장소 조회. 건물 Id가 없으면 전체 검색, 있으면 해당 건물에 속한 시설 검색")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
        content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<List<GlobalSearchRes>> globalSearch(
        @Parameter(name = "building_id", description = "태그화된 건물의 ID(생략 가능)", example = "1", required = false)
        @RequestParam(name = "building_id", required = false) Long buildingId,
        @Parameter(name = "keyword", description = "검색 키워드", example = "애기능", required = true)
        @RequestParam(name = "keyword") String keyword) {
        return CommonResponse.success(searchService.globalSearch(buildingId, keyword));
    }

    /**
     * 건물, 강의실 검색
     * @param placeType 장소 종류
     * @param id 장소 id
     */
    @GetMapping("/place")
    @Operation(summary = "건물, 강의실 조회 결과", description = "특정 건물 또는 강의실 하나에 대한 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchPlaceRes> searchPlace(
        @Parameter(name = "placeType", description = "건물 또는 강의실", example = "BUILDING", required = true)
        @RequestParam PlaceType placeType,
        @Parameter(name = "id", description = "건물이나 강의실의 id", required = true)
        @RequestParam Long id) {
        return CommonResponse.success(searchService.searchPlace(placeType, id));
    }

    /**
     * 편의시설 검색
     * @param buildingId 건물 id
     * @param facilityType 편의시설 종류
     */
    @Operation(summary = "편의시설 조회 결과", description = "특정 편의시설에 대한 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/facilities")
    public CommonResponse<SearchFacilityRes> searchFacility(
        @Parameter(name = "building_id", description = "편의시설이 위치한 건물 id", example = "1", required = true)
        @RequestParam(name = "building_id") Long buildingId,
        @Parameter(name = "facilityType", description = "찾고자 하는 편의시설 종류", example = "TRASH_CAN", required = true)
        @RequestParam FacilityType facilityType) {
        return CommonResponse.success(searchService.searchFacility(buildingId, facilityType));
    }

    /**
     * 검색 기록 조회
     * @param userDetail 사용자 정보
     * @return 사용자가 최근에 검색한 장소명 10개
     */
    @Operation(summary = "검색 기록 조회", description = "유저의 검색 기록 조회: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/logs")
    public CommonResponse<List<GetSearchLogRes>> getSearchLog(@AuthenticationPrincipal UserDetailsImpl userDetail) {
        List<GetSearchLogRes> resList = new ArrayList<>();
        if(userDetail != null) resList = searchService.getSearchLog(userDetail.getUser().getUserId());
        return CommonResponse.success(resList);
    }

    /**
     * 검색 기록 저장
     * @param userDetail 사용자 정보
     * @param req 검색한 내용 정보
     */
    @Operation(summary = "검색 기록 저장", description = "유저의 검색 기록 저장: 로그인 필요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("/logs")
    public CommonResponse<SaveSearchLogRes> saveSearchLog(@AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "검색결과 id, 이름, type", required = true)
        @RequestBody SaveSearchLogReq req) {
        searchService.saveSearchLog(userDetail.getUser().getUserId(), req);
        return CommonResponse.success(new SaveSearchLogRes());
    }

}
