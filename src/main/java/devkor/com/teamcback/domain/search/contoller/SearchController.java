package devkor.com.teamcback.domain.search.contoller;

import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.*;
import devkor.com.teamcback.domain.search.entity.Koyeon;
import devkor.com.teamcback.domain.search.service.SearchService;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    /***
     * 고연전 여부 반환
     */
    @GetMapping("/koyeon")
    @Operation(summary = "고연전 시즌 여부를 t/f로 반환", description = "고연전 시즌 여부를 t/f로 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<Koyeon> isKoyeon() {
        return CommonResponse.success(searchService.isKoyeon());
    }

    /***
     * keyword가 포함된 장소 검색
     * @param keyword 검색 단어
     */
    @GetMapping()
    @Operation(summary = "입력 통한 후보 검색어 조회 (태그X)", description = "keyword가 포함된 장소 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GlobalSearchListRes> globalSearch(
        @Parameter(name = "keyword", description = "검색 키워드", example = "애기능", required = true)
        @RequestParam(name = "keyword") String keyword) {
        return CommonResponse.success(searchService.globalSearch(keyword.trim()));
    }

    /**
     * 모든 or 편의시설이 있는 건물 검색
     */
    @GetMapping("/buildings")
    @Operation(summary = "건물 조회 결과", description = "건물의 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchBuildingListRes> searchBuildings(
        @Parameter(name = "placeType", description = "검색하는 편의시설 종류가 있는 건물을 검색하는 경우", example = "TRASH_CAN")
        @RequestParam(name = "placeType", required = false) PlaceType placeType
    ) {
        return CommonResponse.success(searchService.searchBuildings(placeType));
    }

    /**
     * 건물에 있는 특정 종류의 편의시설 검색
     * @param buildingId 건물 id
     * @param placeType 편의시설 종류
     */
    @GetMapping("/buildings/{buildingId}/facilities")
    @Operation(summary = "건물 내 특정 종류의 편의시설 조회 결과", description = "건물 내 특정 종류의 편의시설에 대한 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchBuildingFacilityListRes> searchBuildingFacilityByType(
        @Parameter(name = "buildingId", description = "편의시설이 위치한 건물 id", example = "1", required = true)
        @PathVariable Long buildingId,
        @Parameter(name = "placeType", description = "찾고자 하는 편의시설 종류", example = "TRASH_CAN", required = true)
        @RequestParam(name = "placeType") PlaceType placeType) {
        return CommonResponse.success(searchService.searchBuildingFacilityByType(buildingId, placeType));
    }

    /**
     * 건물에 있는 편의시설 종류
     * @param buildingId 건물 id
     */
    @GetMapping("/buildings/{buildingId}/facilities/type")
    @Operation(summary = "건물에 있는 편의시설 종류 조회 결과", description = "건물 내 존재하는 편의시설의 종류 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchFacilityTypeRes> searchFacilityTypeByBuilding(
        @Parameter(name = "buildingId", description = "편의시설이 위치한 건물 id", example = "1", required = true)
        @PathVariable Long buildingId) {
        return CommonResponse.success(searchService.searchFacilityTypeByBuilding(buildingId));
    }

    /**
     * 건물 특정 층에 있는 강의실과 편의시설
     * @param buildingId 건물 id
     * @param floor 층
     */
    @GetMapping("/buildings/{buildingId}/floor/{floor}/rooms")
    @Operation(summary = "건물의 특정 층에 있는 강의실과 편의시설 조회 결과", description = "건물 내 특정 층에 있는 강의실과 편의시설 상세 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchRoomRes> searchPlaceByBuildingFloor(
        @Parameter(name = "buildingId", description = "건물 id", example = "1", required = true)
        @PathVariable Long buildingId,
        @Parameter(name = "floor", description = "건물 층", example = "1", required = true)
        @PathVariable int floor) {
        return CommonResponse.success(searchService.searchPlaceByBuildingFloor(buildingId, floor));
    }

    /**
     * 특정 편의시설 리스트
     * @param placeType 편의시설 종류
     */
    @Operation(summary = "특정 종류의 편의시설 목록 조회", description = "특정 종류의 편의시설 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
    })
    @GetMapping("/facilities")
    public CommonResponse<SearchFacilityListRes> searchFacilitiesWithType(
        @Parameter(name = "placeType", description = "검색하는 편의시설 종류", example = "CAFE", required = true)
        @RequestParam(name = "placeType") PlaceType placeType) {
        return CommonResponse.success(searchService.searchFacilitiesWithType(placeType));
    }

    /**
     * Mask Index 대응 교실 조회
     * @param buildingId 건물 id
     * @param floor 건물 층
     * @param maskIndex 교실 mask index
     */
    @Operation(summary = "Mask Index 대응 장소 조회", description = "Room의 mask index에 대응되는 장소 id를 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/buildings/{buildingId}/floor/{floor}/mask/{maskIndex}")
    public CommonResponse<SearchPlaceByMaskIndexRes> searchPlaceByMaskIndex(
        @Parameter(name = "buildingId", description = "건물 id", example = "1", required = true)
        @PathVariable Long buildingId,
        @Parameter(name = "floor", description = "건물 층", example = "1", required = true)
        @PathVariable int floor,
        @Parameter(name = "maskIndex", description = "교실 mask index", example = "5", required = true)
        @PathVariable Integer maskIndex) {
        return CommonResponse.success(searchService.searchPlaceByMaskIndex(buildingId, floor, maskIndex));
    }

    /**
     * place 대응 Mask Index 반환
     * @param placeId 장소 id
     */
    @Operation(summary = "장소 대응 Mask Index 조회", description = "Place의 id에 대응되는 mask index를 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/place/{placeId}/mask")
    public CommonResponse<SearchMaskIndexByPlaceRes> searchMaskIndexByPlace(
        @Parameter(name = "placeId", description = "placeId", example = "1", required = true)
        @PathVariable Long placeId) {
        return CommonResponse.success(searchService.searchMaskIndexByPlace(placeId));
    }

    /**
     * 건물 상세 정보 조회
     * @param buildingId 건물 id
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "건물 상세 정보 조회", description = "건물 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/buildings/{buildingId}")
    public CommonResponse<SearchBuildingDetailRes> searchBuildingDetail(
        @Parameter(description = "사용자정보", required = false)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(name = "buildingId", description = "건물 id", example = "1", required = true)
        @PathVariable Long buildingId) {
        Long userId = (userDetail != null) ? userDetail.getUser().getUserId() : null;
        return CommonResponse.success(searchService.searchBuildingDetail(userId, buildingId));
    }

    /**
     * 장소 상세 정보 조회
     * @param placeId 장소 id
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "장소 상세 정보 조회", description = "장소 상세 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/place/{placeId}")
    public CommonResponse<SearchPlaceDetailRes> searchPlaceDetail(
        @Parameter(description = "사용자정보", required = false)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(name = "placeId", description = "placeId", example = "1", required = true)
        @PathVariable Long placeId) {
        Long userId = (userDetail != null) ? userDetail.getUser().getUserId() : null;
        return CommonResponse.success(searchService.searchPlaceDetail(userId, placeId));
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
    public CommonResponse<SearchLogListRes> getSearchLog(@AuthenticationPrincipal UserDetailsImpl userDetail) {
        List<SearchLogRes> resList = new ArrayList<>();
        if(userDetail != null) resList = searchService.getSearchLog(userDetail.getUser().getUserId());
        return CommonResponse.success(new SearchLogListRes(resList));
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
