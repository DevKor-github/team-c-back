package devkor.com.teamcback.domain.place.controller;

import devkor.com.teamcback.domain.place.dto.response.*;
import devkor.com.teamcback.domain.place.service.AdminPlaceImageService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/places")
public class AdminPlaceImageController {
    private final AdminPlaceImageService adminPlaceImageService;

    @PostMapping(value = "/{placeId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "장소 사진 1장 추가",
        description = "장소 사진 1장 추가 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SavePlaceImageRes> savePlaceImage(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
        @Parameter(description = "저장할 사진 파일") @RequestPart("image") MultipartFile image
    ) {
        return CommonResponse.success(adminPlaceImageService.savePlaceImage(placeId, image));
    }

    @PostMapping(value = "/{placeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "장소 사진 여러장 새로 저장",
            description = "기존 장소 사진 전체 삭제 후 사진 여러장 새로 저장")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SavePlaceImageRes> savePlaceImageList(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
            @Parameter(description = "저장할 사진 파일 목록") @RequestPart("image") List<MultipartFile> images
    ) {
        return CommonResponse.success(adminPlaceImageService.savePlaceImageList(placeId, images));
    }

    @DeleteMapping(value = "/{placeId}/images")
    @Operation(summary = "장소 사진 전체 삭제",
        description = "장소 사진 전체 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeletePlaceImageRes> deletePlaceImage(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId
    ) {
        return CommonResponse.success(adminPlaceImageService.deletePlaceImage(placeId));
    }

    @GetMapping("/{placeId}/image")
    @Operation(summary = "장소 사진 1장 검색",
        description = "장소 사진 1장 검색")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetPlaceImageRes> getPlaceImage(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId
    ) {
        return CommonResponse.success(adminPlaceImageService.searchPlaceImage(placeId));
    }

    @GetMapping("/{placeId}/images")
    @Operation(summary = "장소 사진 여러 장 검색",
            description = "장소 사진 여러 장 검색")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchPlaceImageListRes> getPlaceImageList(
            @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId
    ) {
        return CommonResponse.success(adminPlaceImageService.searchPlaceImageList(placeId));
    }
}
