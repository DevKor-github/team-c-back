package devkor.com.teamcback.domain.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import devkor.com.teamcback.global.config.QueryDslConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class) // @Repository인 QueryDSL 커스텀 구현체 스캔에 필요
class UserRepositoryBackfillTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    @DisplayName("score와 불일치하는 level만 백필하고, 재실행 시 0건 (멱등)")
    @Test
    void backfillLevels() {
        User staleUser = userRepository.save(new User("stale", "stale@test.com", Role.USER, Provider.KAKAO));
        staleUser.updateScore(60L, false); // level = LEVEL5
        User freshUser = userRepository.save(new User("fresh", "fresh@test.com", Role.USER, Provider.KAKAO));
        freshUser.updateScore(10L, false); // level = LEVEL2
        em.flush();

        // 컬럼 도입 이전의 레거시 상태(레벨 불일치)를 native SQL로 재현
        em.createNativeQuery("UPDATE tb_user SET level = 'LEVEL1' WHERE username = 'stale'").executeUpdate();
        em.clear();

        int updated = userRepository.backfillLevels();
        em.clear();

        assertEquals(1, updated);
        assertEquals(Level.LEVEL5, userRepository.findByUserId(staleUser.getUserId()).getLevel());
        assertEquals(Level.LEVEL2, userRepository.findByUserId(freshUser.getUserId()).getLevel());

        // 두 번째 실행은 아무것도 갱신하지 않음
        assertEquals(0, userRepository.backfillLevels());
    }

    @DisplayName("point가 NULL인 레거시 행만 score 값으로 백필 (사용 후 0이 된 잔액은 재적립 안 됨)")
    @Test
    void backfillPoints() {
        User legacyUser = userRepository.save(new User("legacy", "legacy@test.com", Role.USER, Provider.KAKAO));
        legacyUser.updateScore(30L, false);
        User spentUser = userRepository.save(new User("spent", "spent@test.com", Role.USER, Provider.KAKAO));
        spentUser.updateScore(30L, false); // point는 자바 초기값 0 (이미 초기화된 사용자로 간주)
        em.flush();

        // 컬럼 도입 이전의 레거시 상태(point NULL)를 native SQL로 재현
        em.createNativeQuery("UPDATE tb_user SET point = NULL WHERE username = 'legacy'").executeUpdate();
        em.clear();

        assertEquals(1, userRepository.backfillPoints());
        em.clear();

        assertEquals(30L, userRepository.findByUserId(legacyUser.getUserId()).getPoint());
        assertEquals(0L, userRepository.findByUserId(spentUser.getUserId()).getPoint()); // NULL 아니면 미변경

        // 두 번째 실행은 아무것도 갱신하지 않음 (멱등)
        assertEquals(0, userRepository.backfillPoints());
    }

    @DisplayName("포인트 차감: 잔액이 충분할 때만 원자적으로 차감")
    @Test
    void deductPoint() {
        User user = userRepository.save(new User("buyer", "buyer@test.com", Role.USER, Provider.KAKAO));
        user.addPoint(10);
        em.flush();

        assertEquals(1, userRepository.deductPoint(user.getUserId(), 7));
        assertEquals(3L, userRepository.findByUserId(user.getUserId()).getPoint());

        // 잔액(3)보다 큰 금액은 차감 실패
        assertEquals(0, userRepository.deductPoint(user.getUserId(), 4));
        assertEquals(3L, userRepository.findByUserId(user.getUserId()).getPoint());
    }
}
