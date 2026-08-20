package devkor.com.teamcback.domain.character.controller;

import devkor.com.teamcback.domain.character.dto.request.CreateCharacterReq;
import devkor.com.teamcback.domain.character.dto.request.ModifyCharacterReq;
import devkor.com.teamcback.domain.character.dto.response.CreateCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.DeleteCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetAdminCharacterListRes;
import devkor.com.teamcback.domain.character.dto.response.GrantCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.ModifyCharacterRes;
import devkor.com.teamcback.domain.character.service.AdminStoreService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/store")
public class AdminStoreController {
    private final AdminStoreService adminStoreService;

    /**
     * 캐릭터 목록 조회 (비활성 포함)
     */
    @Operation(summary = "관리자 캐릭터 목록 조회", description = "비활성 캐릭터를 포함한 전체 캐릭터 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
    })
    @GetMapping("")
    public CommonResponse<GetAdminCharacterListRes> getCharacterList() {
        return CommonResponse.success(adminStoreService.getCharacterList());
    }

    /**
     * 캐릭터 생성
     * @param req 캐릭터 정보 (이미지, 가격 포함)
     */
    @Operation(summary = "캐릭터 생성", description = "캐릭터 생성 (이미지 필수, 가격은 0 이상의 포인트)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "400", description = "잘못된 입력",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<CreateCharacterRes> createCharacter(
        @Parameter(description = "캐릭터 정보")
        @ModelAttribute CreateCharacterReq req) {
        return CommonResponse.success(adminStoreService.createCharacter(req));
    }

    /**
     * 캐릭터 수정
     * @param characterId 캐릭터 ID
     * @param req 캐릭터 정보 (이미지 미첨부 시 기존 이미지 유지)
     */
    @Operation(summary = "캐릭터 수정", description = "캐릭터 수정 (이미지 미첨부 시 기존 이미지 유지)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PutMapping(value = "/{characterId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<ModifyCharacterRes> modifyCharacter(
        @Parameter(description = "캐릭터 ID", example = "1")
        @PathVariable(name = "characterId") Long characterId,
        @Parameter(description = "캐릭터 정보")
        @ModelAttribute ModifyCharacterReq req) {
        return CommonResponse.success(adminStoreService.modifyCharacter(characterId, req));
    }

    /**
     * 캐릭터 삭제
     * @param characterId 캐릭터 ID
     */
    @Operation(summary = "캐릭터 삭제", description = "캐릭터 삭제 (구매한 사용자가 있으면 삭제 불가, isActive=false로 숨김 처리 권장)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "409", description = "구매한 사용자가 있는 캐릭터",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @DeleteMapping("/{characterId}")
    public CommonResponse<DeleteCharacterRes> deleteCharacter(
        @Parameter(description = "캐릭터 ID", example = "1")
        @PathVariable(name = "characterId") Long characterId) {
        return CommonResponse.success(adminStoreService.deleteCharacter(characterId));
    }

    /**
     * 캐릭터 수동 지급
     * @param characterId 캐릭터 ID
     * @param userId 사용자 ID
     */
    @Operation(summary = "캐릭터 수동 지급", description = "특정 사용자에게 캐릭터 지급 (이벤트 보상용, 포인트 차감 없음)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 보유한 캐릭터",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    @PostMapping("/{characterId}/grant/{userId}")
    public CommonResponse<GrantCharacterRes> grantCharacter(
        @Parameter(description = "캐릭터 ID", example = "1")
        @PathVariable(name = "characterId") Long characterId,
        @Parameter(description = "사용자 ID", example = "1")
        @PathVariable(name = "userId") Long userId) {
        return CommonResponse.success(adminStoreService.grantCharacter(characterId, userId));
    }
}
