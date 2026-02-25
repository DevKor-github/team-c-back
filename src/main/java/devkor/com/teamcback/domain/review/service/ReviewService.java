package devkor.com.teamcback.domain.review.service;

import devkor.com.teamcback.domain.common.entity.File;
import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.review.dto.request.CreateReviewReq;
import devkor.com.teamcback.domain.review.dto.request.ModifyReviewReq;
import devkor.com.teamcback.domain.review.dto.response.*;
import devkor.com.teamcback.domain.review.entity.*;
import devkor.com.teamcback.domain.review.repository.PlaceReviewTagMapRepository;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.review.repository.ReviewTagMapRepository;
import devkor.com.teamcback.domain.review.repository.ReviewTagRepository;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceImageRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceReviewTagRes;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import devkor.com.teamcback.infra.s3.FilePath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final ReviewTagMapRepository reviewTagMapRepository;
    private final PlaceReviewTagMapRepository placeReviewTagMapRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileUtil fileUtil;

    /**
     * 리뷰 태그 종류 검색
     */
    public GetReviewTagListRes getReviewTagList() {
        Map<String, List<GetReviewTagRes>> reviewTagMap = new HashMap<>();

        // 태그 종류
        for(TagType tagType : TagType.values()) {
            reviewTagMap.put(tagType.name(), new ArrayList<>());
        }

        // 태그 종류별 내용
        List<ReviewTag> reviewTagList = reviewTagRepository.findAll();
        for(ReviewTag reviewTag : reviewTagList) {
            reviewTagMap.get(reviewTag.getType().name()).add(new GetReviewTagRes(reviewTag));
        }

        return new GetReviewTagListRes(reviewTagMap);
    }

    /**
     * 리뷰 기능있는 장소 상세 조회
     */
    @Transactional(readOnly = true)
    public GetReviewPlaceDetailRes getReviewPlaceDetail(Long placeId) {
        // 장소 검색
        Place place = findPlaceById(placeId);

        // 식당, 카페만 조회 가능하도록 제한
        if(place.getType() != PlaceType.CAFETERIA && place.getType() != PlaceType.CAFE
                && place.getType() != PlaceType.CAFE_TEMP && place.getType() != PlaceType.CAFT_TEMP && place.getType() != PlaceType.CONV_TEMP) {
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

        // 리뷰별 이미지 조회(썸네일)
        List<SearchPlaceReviewRes> reviewImageList = new ArrayList<>();

        // 리뷰별 이미지 조회
        for(Review review : reviewList) {
            // 리뷰별 이미지
            List<File> reviewFiles = fileUtil.getFiles(review.getFileUuid());

            // 썸네일 이미지 추출
            List<SearchReviewImageRes> imageResList = reviewFiles.stream().map(file -> new SearchReviewImageRes(file.getId(), file.getThumbSavedName())).toList();
            reviewImageList.add(new SearchPlaceReviewRes(review, imageResList));
        }

        // 리뷰 전체 이미지 조회(원본 - 10장까지)
        List<SearchReviewImageRes> totalReviewImageList = fileRepository.getReviewFilesByPlaceWithPage(placeId, 0L, 10).stream().map(file -> new SearchReviewImageRes(file.getId(), file.getFileSavedName())).toList();

        return new GetReviewPlaceDetailRes(place, placeImageList, reviewTagList, reviewImageList, totalReviewImageList);
    }

    /**
     * 리뷰 기능있는 장소 상세 조회 - 리뷰 사진 추가 조회
     */
    @Transactional(readOnly = true)
    public List<SearchReviewImageRes> getReviewPlaceDetailImages(Long placeId, Long lastFileId) {
        // 장소 검색
        Place place = findPlaceById(placeId);

        // 식당, 카페만 조회 가능하도록 제한
        if(place.getType() != PlaceType.CAFETERIA && place.getType() != PlaceType.CAFE) {
            throw new GlobalException(ResultCode.NOT_SUPPORTED_PLACE_TYPE);
        }

        // 리뷰 전체 이미지 조회(원본 - 10장까지)
        return fileRepository.getReviewFilesByPlaceWithPage(placeId, lastFileId, 10).stream().map(file -> new SearchReviewImageRes(file.getId(), file.getFileSavedName())).toList();

    }

    /**
     * 리뷰 작성
     * - 기본 점수: +3점 (별점)
     * - 한줄평 10글자 이상: +7점
     * - 사진 1장 이상: +3점
     * - 같은 장소 하루 1개 리뷰 제한
     */
    @Transactional
    @UpdateScore(dynamic = true)
    public CreateReviewRes createReview(Long userId, Long placeId, CreateReviewReq createReviewReq) {
        // 한줄평 길이 검증 (작성했다면 10글자 이상)
        validateCommentLength(createReviewReq.getComment());

        // 사용자 검색
        User user = findUserById(userId);

        // 장소 검색
        Place place = findPlaceById(placeId);

        // 리뷰 저장
        Review savedReview = reviewRepository.save(new Review(createReviewReq, user, place));

        // 사진 저장
        fileUtil.upload(createReviewReq.getImages(), savedReview.getFileUuid(), null, FilePath.REVIEW);

        // 리뷰 태그 저장
        saveReviewTagMap(savedReview, createReviewReq.getTagIds());

        // 장소 별점 추가
        place.setStarNum(place.getStarNum() + 1);
        place.setStarSum(place.getStarSum() + createReviewReq.getScore());

        return new CreateReviewRes(savedReview.getId());
    }

    /**
     * 리뷰 조회
     */
    @Transactional(readOnly = true)
    public GetReviewRes getReview(Long reviewId) {
        // 리뷰 검색
        Review review = findReviewById(reviewId);

        // 리뷰 태그 조회
        List<GetReviewTagRes> tagResList = new ArrayList<>();
        for(ReviewTagMap reviewTagMap : review.getReviewTagMaps()) {
            tagResList.add(new GetReviewTagRes(reviewTagMap.getReviewTag()));
        }

        // 리뷰 이미지 조회
        List<SearchReviewImageRes> imageResList = fileUtil.getFiles(review.getFileUuid()).stream().map(file -> new SearchReviewImageRes(file.getId(), file.getFileSavedName())).toList();

        return new GetReviewRes(review, tagResList, imageResList);
    }

    /**
     * 리뷰 수정
     * - 한줄평/사진 추가 시 추가 점수 부여
     * - 한줄평/사진 제거 시 해당 점수 차감
     */
    @Transactional
    @UpdateScore(dynamic = true)
    public ModifyReviewRes modifyReview(Long userId, Long reviewId, @Valid ModifyReviewReq modifyReviewReq) {
        // 한줄평 길이 검증 (작성했다면 10글자 이상)
        validateCommentLength(modifyReviewReq.getComment());

        // 사용자 검색
        User user = findUserById(userId);

        // 리뷰 검색
        Review review = findReviewById(reviewId);

        // 권한 검사 (자신이 작성한 리뷰만 수정 가능)
        validateUser(user, review);

        // 사진 삭제
        fileUtil.deleteFile(review.getFileUuid());

        // 사진 저장
        fileUtil.upload(modifyReviewReq.getImages(), review.getFileUuid(), null, FilePath.REVIEW);

        // 리뷰 수정
        review.modify(modifyReviewReq);

        // 기존 태그 삭제
        deleteReviewTagMap(review);

        // 태그 저장
        saveReviewTagMap(review, modifyReviewReq.getTagIds());

        // 장소 평점 수정
        review.getPlace().setStarSum(review.getPlace().getStarSum() - review.getScore() + modifyReviewReq.getScore());

        return new ModifyReviewRes(review.getId());
    }

    /**
     * 리뷰 삭제
     * - 삭제한 리뷰의 점수만큼 차감 (최소 0점)
     */
    @Transactional
    @UpdateScore(dynamic = true)
    public DeleteReviewRes deleteReview(Long userId, Long reviewId) {
        // 사용자 검색
        User user = findUserById(userId);

        // 리뷰 검색
        Review review = findReviewById(reviewId);

        // 권한 검사 (자신이 작성한 리뷰만 삭제 가능)
        validateUser(user, review);

        // 리뷰 태그 삭제
        deleteReviewTagMap(review);

        // 사진 삭제
        fileUtil.deleteFile(review.getFileUuid());

        // 장소 평정 수정
        review.getPlace().setStarNum(review.getPlace().getStarNum() - 1);
        review.getPlace().setStarSum(review.getPlace().getStarSum() - review.getScore());

        // 리뷰 삭제
        reviewRepository.delete(review);

        return new DeleteReviewRes();
    }

    /**
     * 장소 검색
     */
    private Place findPlaceById(Long id) {
        return placeRepository.findById(id).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
    }

    /**
     * 사용자 검색
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
    }

    /**
     * 리뷰 검색
     */
    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_REVIEW));
    }

    /**
     * 장소별 리뷰 태그 조회
     */
    private List<SearchPlaceReviewTagRes> findPlaceReviewTagList(Place place) {
        List<PlaceReviewTagMap> reviewTagMaps = placeReviewTagMapRepository.findByPlaceOrderByNumDesc(place);

        List<SearchPlaceReviewTagRes> tagResList = new ArrayList<>();
        for(PlaceReviewTagMap reviewTagMap : reviewTagMaps) {
            tagResList.add(new SearchPlaceReviewTagRes(reviewTagMap.getReviewTag().getId(), reviewTagMap.getReviewTag().getTag(), reviewTagMap.getNum()));
        }

        return tagResList;
    }

    /**
     * 리뷰 권한 검사
     */
    private void validateUser(User user, Review review) {
        // 권한 검사 (자신이 작성한 리뷰만 수정 가능)
        if(!user.getUserId().equals(review.getUser().getUserId())) {
            throw new GlobalException(ResultCode.UNAUTHORIZED);
        }
    }

    /**
     * 한줄평 길이 검증 (작성했다면 10글자 이상)
     */
    private void validateCommentLength(String comment) {
        if(comment != null && !comment.isBlank() && comment.length() < 10) {
            throw new GlobalException(ResultCode.COMMENT_TOO_SHORT);
        }
    }

    /**
     * 리뷰 태그 저장
     */
    private void saveReviewTagMap(Review review, List<Long> reviewTagIds) {

        // 태그 저장
        List<ReviewTagMap> reviewTagMaps = new ArrayList<>();
        for(Long tagId : reviewTagIds) {
            ReviewTag tag = reviewTagRepository.findById(tagId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_REVIEW_TAG));

            // 리뷰 태그 저장
            ReviewTagMap reviewTagMap = reviewTagMapRepository.save(new ReviewTagMap(review, tag));
            reviewTagMaps.add(reviewTagMap);

            // 장소 태그 저장
            PlaceReviewTagMap placeReviewTagMap = placeReviewTagMapRepository.findByPlaceAndReviewTag(review.getPlace(), tag);

            if(placeReviewTagMap != null) {
                placeReviewTagMap.setNum(placeReviewTagMap.getNum() + 1);
            }
            else {
                placeReviewTagMapRepository.save(new PlaceReviewTagMap(review.getPlace(), tag));
            }
        }

        // 리뷰에 태그 목록 저장
        review.setReviewTagMaps(reviewTagMaps);
    }

    /**
     * 기존 리뷰 태그 삭제
     */
    private void deleteReviewTagMap(Review review) {
        // 기존 태그 삭제
        List<ReviewTagMap> reviewTagMaps = review.getReviewTagMaps();
        review.setReviewTagMaps(null);

        for(ReviewTagMap reviewTagMap : reviewTagMaps) {
            // 장소 태그 수정
            PlaceReviewTagMap placeReviewTagMap = placeReviewTagMapRepository.findByPlaceAndReviewTag(review.getPlace(), reviewTagMap.getReviewTag());
            placeReviewTagMap.setNum(placeReviewTagMap.getNum() - 1);
            if(placeReviewTagMap.getNum() <= 0) {
                placeReviewTagMapRepository.delete(placeReviewTagMap);
            }

            // 리뷰 태그 삭제
            reviewTagMapRepository.delete(reviewTagMap);
        }
    }
}
