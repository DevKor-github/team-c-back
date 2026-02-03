package devkor.com.teamcback.domain.place.controller;

import devkor.com.teamcback.domain.place.dto.response.DeletePlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.ModifyPlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.SavePlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.SearchPlaceImageRes;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/places")
public class AdminPlaceImageController {
    private final AdminPlaceImageService adminPlaceImageService;

    @PostMapping(value = "/{placeId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "장소 사진 저장",
        description = "장소 사진 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "409", description = "해당 장소의 이미지가 이미 존재합니다.",
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

    @PutMapping(value = "/{placeId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "장소 사진 수정",
        description = "장소 사진 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyPlaceImageRes> modifyPlaceImage(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId,
        @Parameter(description = "수정할 사진 파일") @RequestPart("image") MultipartFile image
    ) {
        return CommonResponse.success(adminPlaceImageService.modifyPlaceImage(placeId, image));
    }

    @DeleteMapping(value = "/{placeId}/image")
    @Operation(summary = "장소 사진 삭제",
        description = "장소 사진 삭제")
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
    @Operation(summary = "장소 사진 검색",
        description = "장소 사진 검색")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SearchPlaceImageRes> getPlaceImage(
        @Parameter(name = "placeId", description = "장소 ID") @PathVariable Long placeId
    ) {
        return CommonResponse.success(adminPlaceImageService.searchPlaceImage(placeId));
    }
}
