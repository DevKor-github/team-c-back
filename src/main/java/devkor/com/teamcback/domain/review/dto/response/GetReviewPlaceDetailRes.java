package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceImageRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceReviewTagRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
public class GetReviewPlaceDetailRes {

    private Long placeId;
    private Long nodeId;
    private Integer maskIndex;
    private PlaceType type;
    private String name;
    private String detail;
    private String operatingTime;
    private boolean availability;
    private boolean plugAvailability;
    private boolean isOperating;
    private String description;

    @Schema(description = "별점 평균")
    private String starAverage;
    @Schema(description = "음식 카테고리", example = "한식")
    private String foodTypeName;

    @Schema(description = "장소 대표 사진 목록")
    private List<SearchPlaceImageRes> placeImages;
    @Schema(description = "리뷰 태그 목록")
    private List<SearchPlaceReviewTagRes> tagList;
    @Schema(description = "리뷰 목록")
    private List<SearchPlaceReviewRes> reviewList;
    @Schema(description = "리뷰 사진 목록")
    private List<SearchReviewImageRes> reviewImageList;

    public GetReviewPlaceDetailRes(Place place, List<SearchPlaceImageRes> placeImageList, List<SearchPlaceReviewTagRes> reviewTagList, List<SearchPlaceReviewRes> reviewList, List<SearchReviewImageRes> reviewImageList) {
        this.placeId = place.getId();
        this.nodeId = place.getNode().getId();
        this.maskIndex = place.getMaskIndex();
        this.type = place.getType();
        this.name = place.getName();
        this.detail = place.getDetail();
        this.operatingTime = place.getOperatingTime();
        this.availability = place.isAvailability();
        this.plugAvailability = place.isPlugAvailability();
        this.isOperating = place.isOperating();
        this.description = place.getDescription();
        this.starAverage = String.format("%.2f", ((double) place.getStarSum()) / place.getStarNum());
        this.foodTypeName = place.getFoodType() == null ? "" : place.getFoodType().getType();
        this.placeImages = placeImageList;
        this.tagList = reviewTagList;
        this.reviewList = reviewList;
        this.reviewImageList = reviewImageList;
    }
}
