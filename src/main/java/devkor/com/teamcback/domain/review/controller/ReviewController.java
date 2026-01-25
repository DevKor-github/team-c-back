package devkor.com.teamcback.domain.review.controller;

import devkor.com.teamcback.domain.review.dto.request.CreateReviewReq;
import devkor.com.teamcback.domain.review.dto.request.ModifyReviewReq;
import devkor.com.teamcback.domain.review.dto.response.*;
import devkor.com.teamcback.domain.review.service.ReviewService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰가 있는 장소 상세 검색",
            description = "식당, 카페")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/places/{placeId}")
    public CommonResponse<GetReviewPlaceDetailRes> getReviewPlaceDetail(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId) {

        return CommonResponse.success(reviewService.getReviewPlaceDetail(placeId));
    }

    @Operation(summary = "리뷰가 있는 장소 상세 검색 - 무한스크롤로 리뷰 사진 추가 조회",
            description = "식당, 카페")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/places/{placeId}/images")
    public CommonResponse<List<SearchReviewImageRes>> getReviewPlaceDetailImages(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
            @Parameter(name = "lastFileId", description = "마지막 조회한 사진 id") @RequestParam Long lastFileId) {

        return CommonResponse.success(reviewService.getReviewPlaceDetailImages(placeId, lastFileId));
    }

    // TODO: 리뷰 작성 시 포인트 부여
    @Operation(summary = "리뷰 작성",
            description = "식당, 카페에 대한 리뷰를 작성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping(value = "/places/{placeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<CreateReviewRes> createReview(
            @Parameter(description = "사용자정보", required = true) @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
            @Parameter(description = "리뷰 작성 내용", required = true) @Valid @ModelAttribute CreateReviewReq createReviewReq) {

        return CommonResponse.success(reviewService.createReview(userDetail.getUser().getUserId(), placeId, createReviewReq));
    }

    @Operation(summary = "리뷰 조회",
            description = "식당, 카페에 대한 리뷰 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/{reviewId}")
    public CommonResponse<GetReviewRes> getReview(
            @Parameter(name = "reviewId", description = "리뷰 ID") @PathVariable Long reviewId) {
        return CommonResponse.success(reviewService.getReview(reviewId));
    }

    @Operation(summary = "리뷰 수정",
            description = "식당, 카페에 대한 리뷰를 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<ModifyReviewRes> modifyReview(
            @Parameter(description = "사용자정보", required = true) @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(name = "reviewId", description = "리뷰 ID") @PathVariable Long reviewId,
            @Parameter(description = "리뷰 작성 내용", required = true) @Valid @ModelAttribute ModifyReviewReq modifyReviewReq) {

        return CommonResponse.success(reviewService.modifyReview(userDetail.getUser().getUserId(), reviewId, modifyReviewReq));
    }

    // TODO: 리뷰 삭제 시 포인트 제거
    @Operation(summary = "리뷰 삭제",
            description = "식당, 카페에 대한 리뷰를 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @DeleteMapping( "/{reviewId}")
    public CommonResponse<DeleteReviewRes> deleteReview(
            @Parameter(description = "사용자정보", required = true) @AuthenticationPrincipal UserDetailsImpl userDetail,
            @Parameter(name = "reviewId", description = "리뷰 ID") @PathVariable Long reviewId) {
        return CommonResponse.success(reviewService.deleteReview(userDetail.getUser().getUserId(), reviewId));
    }
}
