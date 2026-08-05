package devkor.com.teamcback.domain.character.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PurchaseStatus {
    OWNED("보유"),
    PURCHASABLE("구매 가능"),
    LOCKED("미해금"), // 해금 레벨 미달 (포인트와 무관하게 구매 불가)
    NOT_ENOUGH_POINT("포인트 부족");

    private final String name;
}
