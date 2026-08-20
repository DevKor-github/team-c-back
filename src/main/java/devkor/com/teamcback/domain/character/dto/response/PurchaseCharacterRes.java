package devkor.com.teamcback.domain.character.dto.response;

import devkor.com.teamcback.domain.character.entity.UserCharacter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;

@Schema(description = "캐릭터 구매 결과")
@Getter
public class PurchaseCharacterRes {
    @Schema(description = "캐릭터 ID", example = "1")
    private Long characterId;
    @Schema(description = "캐릭터 이름", example = "아기 호랑이")
    private String name;
    @Schema(description = "캐릭터 대사", example = "같이 캠퍼스를 누벼볼까?")
    private String quote;
    @Schema(description = "캐릭터 이미지 URL")
    private String imageUrl;
    @Schema(description = "지불한 포인트", example = "10")
    private Integer price;
    @Schema(description = "구매 후 잔여 포인트", example = "15")
    private Long remainingPoint;
    @Schema(description = "구매 일시")
    private LocalDateTime purchasedAt;

    public PurchaseCharacterRes(UserCharacter userCharacter, Long remainingPoint) {
        this.characterId = userCharacter.getCharacter().getCharacterId();
        this.name = userCharacter.getCharacter().getName();
        this.quote = userCharacter.getCharacter().getQuote();
        this.imageUrl = userCharacter.getCharacter().getImageUrl();
        this.price = userCharacter.getCharacter().getPrice();
        this.remainingPoint = remainingPoint;
        this.purchasedAt = userCharacter.getCreatedAt();
    }
}
