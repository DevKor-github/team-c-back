package devkor.com.teamcback.domain.character.controller;

import devkor.com.teamcback.domain.character.dto.response.EquipCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetMyCharacterListRes;
import devkor.com.teamcback.domain.character.dto.response.GetStoreRes;
import devkor.com.teamcback.domain.character.dto.response.PurchaseCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.UnequipCharacterRes;
import devkor.com.teamcback.domain.character.service.StoreService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
public class StoreController {
    private final StoreService storeService;

    /**
     * 스토어 조회 (보유 포인트 + 캐릭터 목록)
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "스토어 조회", description = "보유 포인트와 전체 캐릭터 목록(보유/구매 가능/포인트 부족) 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("")
    public CommonResponse<GetStoreRes> getStore(
        @Parameter(description = "사용자 정보")
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        return CommonResponse.success(storeService.getStore(userDetail.getUser().getUserId()));
    }

    /**
     * 내 보유 캐릭터 목록 조회
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "보유 캐릭터 목록 조회", description = "보유 포인트, 구매한 캐릭터, 대표 장착 캐릭터 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @GetMapping("/my")
    public CommonResponse<GetMyCharacterListRes> getMyCharacters(
        @Parameter(description = "사용자 정보")
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        return CommonResponse.success(storeService.getMyCharacters(userDetail.getUser().getUserId()));
    }

    /**
     * 캐릭터 구매
     * @param userDetail 사용자 정보
     * @param characterId 캐릭터 ID
     */
    @Operation(summary = "캐릭터 구매", description = "보유 포인트를 차감하여 캐릭터 구매")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "포인트 부족 또는 비활성 캐릭터",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 보유한 캐릭터",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("/{characterId}/purchase")
    public CommonResponse<PurchaseCharacterRes> purchaseCharacter(
        @Parameter(description = "사용자 정보")
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "캐릭터 ID", example = "1")
        @PathVariable(name = "characterId") Long characterId) {
        return CommonResponse.success(storeService.purchaseCharacter(userDetail.getUser().getUserId(), characterId));
    }

    /**
     * 대표 캐릭터 장착
     * @param userDetail 사용자 정보
     * @param characterId 캐릭터 ID
     */
    @Operation(summary = "대표 캐릭터 장착", description = "구매한 캐릭터를 대표 캐릭터로 장착")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "보유하지 않은 캐릭터",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping("/{characterId}/equip")
    public CommonResponse<EquipCharacterRes> equipCharacter(
        @Parameter(description = "사용자 정보")
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "캐릭터 ID", example = "1")
        @PathVariable(name = "characterId") Long characterId) {
        return CommonResponse.success(storeService.equipCharacter(userDetail.getUser().getUserId(), characterId));
    }

    /**
     * 대표 캐릭터 장착 해제
     * @param userDetail 사용자 정보
     */
    @Operation(summary = "대표 캐릭터 장착 해제", description = "대표 캐릭터 장착 해제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @DeleteMapping("/equip")
    public CommonResponse<UnequipCharacterRes> unequipCharacter(
        @Parameter(description = "사용자 정보")
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        return CommonResponse.success(storeService.unequipCharacter(userDetail.getUser().getUserId()));
    }
}
