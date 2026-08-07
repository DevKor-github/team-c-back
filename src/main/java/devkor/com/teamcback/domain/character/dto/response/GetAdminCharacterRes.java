package devkor.com.teamcback.domain.character.dto.response;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;

@Schema(description = "관리자용 캐릭터 정보")
@Getter
public class GetAdminCharacterRes {
    @Schema(description = "캐릭터 ID", example = "1")
    private Long characterId;
    @Schema(description = "캐릭터 이름", example = "아기 호랑이")
    private String name;
    @Schema(description = "캐릭터 설명")
    private String description;
    @Schema(description = "캐릭터 대사")
    private String quote;
    @Schema(description = "캐릭터 이미지 URL")
    private String imageUrl;
    @Schema(description = "구매 가격 (포인트)", example = "10")
    private Integer price;
    @Schema(description = "해금 레벨", example = "2")
    private Integer requiredLevel;
    @Schema(description = "정렬 순서", example = "1")
    private Integer displayOrder;
    @Schema(description = "노출 여부", example = "true")
    private boolean isActive;
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    public GetAdminCharacterRes(KoCharacter character) {
        this.characterId = character.getCharacterId();
        this.name = character.getName();
        this.description = character.getDescription();
        this.quote = character.getQuote();
        this.imageUrl = character.getImageUrl();
        this.price = character.getPrice();
        this.requiredLevel = character.getRequiredLevel();
        this.displayOrder = character.getDisplayOrder();
        this.isActive = character.isActive();
        this.createdAt = character.getCreatedAt();
    }
}
