package devkor.com.teamcback.domain.character.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

// createdAt(BaseEntity)이 획득일. UNIQUE(user_id, character_id)가 동시 중복 해금의 최종 방어선
@Entity
@Getter
@Table(name = "tb_user_character",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_character", columnNames = {"user_id", "character_id"}))
@NoArgsConstructor
public class UserCharacter extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCharacterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private KoCharacter character;

    public UserCharacter(User user, KoCharacter character) {
        this.user = user;
        this.character = character;
    }
}
