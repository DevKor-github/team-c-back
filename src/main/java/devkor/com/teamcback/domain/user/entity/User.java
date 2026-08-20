package devkor.com.teamcback.domain.user.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "tb_user")
@NoArgsConstructor
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    private Long score = 0L;

    // 스토어에서 사용하는 차감형 재화. score(레벨용 누적치)와 같은 양으로 적립되고 구매 시에만 차감된다.
    // 컬럼을 nullable로 두는 이유: 기존 행은 NULL로 생성되고, NULL 여부가 "백필 전" 표식이 되어 백필이 영원히 멱등해진다.
    private Long point = 0L;

    // MySQL native enum 타입 대신 VARCHAR로 저장하여 레벨 추가 시 ALTER 없이 확장 가능하게 유지
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'LEVEL1'")
    @Column(nullable = false)
    private Level level = Level.LEVEL1;

    @Column(nullable = false)
    private boolean isUpgraded = false;

    // 대표 캐릭터 (tb_character 논리 참조, 미장착 시 null)
    private Long equippedCharacterId;

    @Setter
    @Column(unique = true)
    private String code;

    public User(String username, String email, Role role, Provider provider) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.score = 0L;
        this.isUpgraded = false;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateScore(Long score, boolean isUpgraded) {
        this.score = score;
        this.level = Level.fromScore(score); // score와 level이 발산하지 않도록 단일 지점에서 갱신
        this.isUpgraded = isUpgraded;
    }

    public void updateUpgraded(boolean isUpgraded) {
        this.isUpgraded = isUpgraded;
    }

    public void syncLevel() {
        this.level = Level.fromScore(this.score);
    }

    public Long getPoint() { // 백필 전 NULL 방어
        return point == null ? 0L : point;
    }

    public void addPoint(long amount) {
        this.point = Math.max(0, getPoint() + amount);
    }

    public void updateEquippedCharacter(Long characterId) {
        this.equippedCharacterId = characterId;
    }

}
