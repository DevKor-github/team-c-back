package devkor.com.teamcback.domain.character.dto.response;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;

@Schema(description = "스토어 캐릭터 정보")
@Getter
public class GetStoreCharacterRes {
    @Schema(description = "캐릭터 ID", example = "1")
    private Long characterId;
    @Schema(description = "캐릭터 이름", example = "아기 호랑이")
    private String name;
    @Schema(description = "캐릭터 설명", example = "10 포인트로 구매할 수 있는 캐릭터")
    private String description;
    @Schema(description = "캐릭터 대사", example = "같이 캠퍼스를 누벼볼까?")
    private String quote;
    @Schema(description = "캐릭터 이미지 URL")
    private String imageUrl;
    @Schema(description = "구매 가격 (포인트)", example = "10")
    private Integer price;
    @Schema(description = "해금 레벨", example = "2")
    private Integer requiredLevel;
    @Schema(description = "구매 상태 (OWNED/PURCHASABLE/LOCKED/NOT_ENOUGH_POINT)", example = "PURCHASABLE")
    private PurchaseStatus status;
    @Schema(description = "구매 일시 (미보유 시 null)")
    private LocalDateTime purchasedAt;
    @Schema(description = "대표 장착 여부", example = "false")
    private boolean isEquipped;

    public GetStoreCharacterRes(KoCharacter character, PurchaseStatus status,
        LocalDateTime purchasedAt, boolean isEquipped) {
        this.characterId = character.getCharacterId();
        this.name = character.getName();
        this.description = character.getDescription();
        this.quote = character.getQuote();
        this.imageUrl = character.getImageUrl();
        this.price = character.getPrice();
        this.requiredLevel = character.getRequiredLevel();
        this.status = status;
        this.purchasedAt = purchasedAt;
        this.isEquipped = isEquipped;
    }
}
