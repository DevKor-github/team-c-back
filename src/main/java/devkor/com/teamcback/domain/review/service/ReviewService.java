package devkor.com.teamcback.domain.review.service;

import devkor.com.teamcback.domain.common.entity.File;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.review.dto.response.GetReviewPlaceDetailRes;
import devkor.com.teamcback.domain.review.dto.response.SearchPlaceReviewRes;
import devkor.com.teamcback.domain.review.dto.response.SearchReviewImageRes;
import devkor.com.teamcback.domain.review.entity.PlaceReviewTagMap;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.review.repository.PlaceReviewTagMapRepository;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceImageRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceReviewTagRes;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;
    private final PlaceReviewTagMapRepository placeReviewTagMapRepository;
    private final FileUtil fileUtil;

    /**
     * 리뷰 기능있는 장소 상세 조회
     */
    @Transactional(readOnly = true)
    public GetReviewPlaceDetailRes getReviewPlaceDetail(Long placeId) {
        // 장소 검색
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));

        // 식당, 카페만 조회 가능하도록 제한
        if(place.getType() != PlaceType.CAFETERIA && place.getType() != PlaceType.CAFE) {
            throw new GlobalException(ResultCode.NOT_SUPPORTED_PLACE_TYPE);
        }

        // 장소 대표 사진 조회 (사진 5장 제한 - 원본 사진)
        List<SearchPlaceImageRes> placeImageList = new ArrayList<>();
        if(place.getFileUuid() != null) {
            placeImageList = fileUtil.getTop5Files(place.getFileUuid()).stream().map(file -> new SearchPlaceImageRes(file.getId(), file.getFileSavedName())).toList();
        }

        // 장소 리뷰 태그 조회
        List<SearchPlaceReviewTagRes> reviewTagList = findPlaceReviewTagList(place);

        // 리뷰 최신순 조회
        List<Review> reviewList = reviewRepository.findAllByPlaceOrderByCreatedAtDesc(place);

        // 리뷰 전체 이미지 조회(원본)
        List<SearchReviewImageRes> totalReviewImageList = new ArrayList<>();
        // 리뷰별 이미지 조회(썸네일)
        List<SearchPlaceReviewRes> reviewImageList = new ArrayList<>();

        // 리뷰별 이미지 조회
        for(Review review : reviewList) {
            // 리뷰별 이미지
            List<File> reviewFiles = fileUtil.getFiles(review.getFileUuid());

            // 썸네일 이미지 추출
            List<SearchReviewImageRes> imageResList = reviewFiles.stream().map(file -> new SearchReviewImageRes(file.getId(), file.getThumbSavedName())).toList();
            reviewImageList.add(new SearchPlaceReviewRes(review, imageResList));

            // 리뷰 전체 이미지(원본으로 총 10장까지만)
            int idx = 0;
            while(totalReviewImageList.size() < 10 && idx < reviewFiles.size()) {
                totalReviewImageList.add(new SearchReviewImageRes(reviewFiles.get(idx).getId(), reviewFiles.get(idx).getFileSavedName()));
                idx++;
            }
        }

        return new GetReviewPlaceDetailRes(place, placeImageList, reviewTagList, reviewImageList, totalReviewImageList);
    }

    private List<SearchPlaceReviewTagRes> findPlaceReviewTagList(Place place) {
        List<PlaceReviewTagMap> reviewTagMaps = placeReviewTagMapRepository.findByPlaceOrderByNumDesc(place);

        List<SearchPlaceReviewTagRes> tagResList = new ArrayList<>();
        for(PlaceReviewTagMap reviewTagMap : reviewTagMaps) {
            tagResList.add(new SearchPlaceReviewTagRes(reviewTagMap.getReviewTag().getId(), reviewTagMap.getReviewTag().getTag(), reviewTagMap.getNum()));
        }

        return tagResList;
    }
}
