package devkor.com.teamcback.domain.character.entity;

import devkor.com.teamcback.domain.character.dto.request.CreateCharacterReq;
import devkor.com.teamcback.domain.character.dto.request.ModifyCharacterReq;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

// java.lang.Character와의 충돌을 피하기 위해 KoCharacter로 명명 (koyeon의 Ko- 접두어 선례)
@Entity
@Getter
@Table(name = "tb_character")
@NoArgsConstructor
public class KoCharacter extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long characterId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String quote; // 캐릭터 클릭 시 보여줄 대사

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    // 기본 캐릭터는 0, 그 외에는 실제 구매 시 차감할 포인트.
    // requiredLevel과 함께 쓰이면 "레벨 달성 후 포인트 구매" 정책이다.
    private Integer price;

    // 해금 레벨: 사용자 레벨이 이 값 이상이어야 구매 가능 (1이면 제한 없음)
    @ColumnDefault("1")
    @Column(nullable = false)
    private Integer requiredLevel = 1;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private boolean isActive = true;

    public KoCharacter(String name, String description, String quote, String imageUrl,
        Integer price, Integer requiredLevel, Integer displayOrder, boolean isActive) {
        this.name = name;
        this.description = description;
        this.quote = quote;
        this.imageUrl = imageUrl;
        this.price = price;
        this.requiredLevel = requiredLevel;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    public KoCharacter(CreateCharacterReq req, String imageUrl) {
        this(req.getName(), req.getDescription(), req.getQuote(), imageUrl, req.getPrice(),
            req.getRequiredLevel(), req.getDisplayOrder(), req.isActive());
    }

    public void update(ModifyCharacterReq req, String imageUrl) {
        this.name = req.getName();
        this.description = req.getDescription();
        this.quote = req.getQuote();
        this.imageUrl = imageUrl;
        this.price = req.getPrice();
        this.requiredLevel = req.getRequiredLevel();
        this.displayOrder = req.getDisplayOrder();
        this.isActive = req.isActive();
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
