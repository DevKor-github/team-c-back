package devkor.com.teamcback.domain.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelTest {

    @DisplayName("점수 구간별 레벨 계산 (0/5/20/40/60)")
    @ParameterizedTest
    @CsvSource({
        "0, LEVEL1", "4, LEVEL1",
        "5, LEVEL2", "19, LEVEL2",
        "20, LEVEL3", "39, LEVEL3",
        "40, LEVEL4", "59, LEVEL4",
        "60, LEVEL5", "999, LEVEL5",
    })
    void fromScore(long score, Level expected) {
        assertEquals(expected, Level.fromScore(score));
    }

    @DisplayName("다음 레벨 체인")
    @Test
    void getNextLevel() {
        assertEquals(Level.LEVEL2, Level.LEVEL1.getNextLevel());
        assertEquals(Level.LEVEL3, Level.LEVEL2.getNextLevel());
        assertEquals(Level.LEVEL4, Level.LEVEL3.getNextLevel());
        assertEquals(Level.LEVEL5, Level.LEVEL4.getNextLevel());
        assertNull(Level.LEVEL5.getNextLevel());
    }

    @DisplayName("updateScore 시 score와 level이 함께 갱신")
    @Test
    void updateScoreSyncsLevel() {
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        assertEquals(Level.LEVEL1, user.getLevel());

        user.updateScore(10L, true);
        assertEquals(Level.LEVEL2, user.getLevel());
        assertEquals(10L, user.getScore());

        // 리뷰 삭제 등으로 점수가 내려가면 레벨도 함께 하락
        user.updateScore(3L, true);
        assertEquals(Level.LEVEL1, user.getLevel());
    }

    @DisplayName("포인트 적립/차감은 0 미만으로 내려가지 않음")
    @Test
    void addPointFloorsAtZero() {
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        assertEquals(0L, user.getPoint());

        user.addPoint(10);
        assertEquals(10L, user.getPoint());

        user.addPoint(-15); // 리뷰 삭제 등으로 적립분보다 큰 차감이 와도 0에서 멈춤
        assertEquals(0L, user.getPoint());
    }
}
