package devkor.com.teamcback.domain.search.contoller;

import devkor.com.teamcback.domain.search.dto.request.SearchFacilityReq;
import devkor.com.teamcback.domain.search.dto.request.SearchPlaceReq;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.GetSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SaveSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public CommonResponse<List<GlobalSearchRes>> globalSearch(@RequestParam(name = "building_id", required = false) Long buildingId,
        @RequestParam(name = "keyword") String keyword) {
        return CommonResponse.success(searchService.globalSearch(buildingId, keyword));
    }

    /**
     * 건물, 강의실 검색
     * @param searchReq 장소 종류, id
     */
    @GetMapping("/place")
    public CommonResponse<SearchPlaceRes> searchPlace(@RequestBody SearchPlaceReq searchReq) {
        return CommonResponse.success(searchService.searchPlace(searchReq));
    }

    /**
     * 편의시설 검색
     * @param searchFacilityReq 건물 id, 편의시설 종류
     */
    @GetMapping("/facilities")
    public CommonResponse<SearchFacilityRes> searchFacility(@RequestBody SearchFacilityReq searchFacilityReq) {
        return CommonResponse.success(searchService.searchFacility(searchFacilityReq));
    }

    /**
     * 검색 기록 조회
     * @param userDetail 사용자 정보
     * @return 사용자가 최근에 검색한 장소명 10개
     */
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
    @PostMapping("/logs")
    public CommonResponse<SaveSearchLogRes> saveSearchLog(@AuthenticationPrincipal UserDetailsImpl userDetail, @RequestBody SaveSearchLogReq req) {
        searchService.saveSearchLog(userDetail.getUser().getUserId(), req);
        return CommonResponse.success(new SaveSearchLogRes());
    }

}
