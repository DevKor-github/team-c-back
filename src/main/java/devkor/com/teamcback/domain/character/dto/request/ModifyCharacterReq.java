package devkor.com.teamcback.domain.character.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "수정할 캐릭터 정보")
@Getter
@Setter
public class ModifyCharacterReq {
    @Schema(description = "캐릭터 이름", example = "아기 호랑이")
    private String name;

    @Schema(description = "캐릭터 설명", example = "10 포인트로 구매할 수 있는 캐릭터")
    private String description;

    @Schema(description = "캐릭터 대사", example = "같이 캠퍼스를 누벼볼까?")
    private String quote;

    @Schema(description = "구매 가격 (포인트)", example = "10")
    private Integer price;

    @Schema(description = "해금 레벨 (1~5, 1이면 제한 없음)", example = "2")
    private Integer requiredLevel = 1;

    @Schema(description = "정렬 순서", example = "1")
    private Integer displayOrder = 0;

    @Schema(description = "노출 여부", example = "true")
    private boolean isActive = true;

    @Schema(description = "캐릭터 이미지 (미첨부 시 기존 이미지 유지)")
    private MultipartFile image;
}
