package devkor.com.teamcback.domain.user.scheduler;

import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * level/point 컬럼 도입에 따른 기존 사용자 백필.
 * - level: score와 불일치하는 행만 교정 (정상 상태에서는 0건 갱신)
 * - point: NULL인 행만 score 값으로 초기화 (사용 후 0이 된 잔액은 다시 채워지지 않음)
 * 매 부팅 시 실행되지만 두 쿼리 모두 멱등.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLevelBackfillRunner {
    private final UserRepository userRepository;
    private final RedisLockUtil redisLockUtil;

    @Value("${metrics.environment}")
    private String env;

    // 트랜잭션 경계는 UserRepository.backfillLevels()에 둔다. 리스너 메서드를 @Transactional로 감싸면
    // UPDATE 실패를 catch해도 rollback-only 커밋 예외가 리스너 밖으로 나가 부팅이 실패한다.
    @EventListener(ApplicationReadyEvent.class)
    public void backfillUserLevels() {
        if(env.equals("test")) return; // H2 테스트 환경은 create-drop이라 백필 불필요 + Redis 미기동

        try {
            redisLockUtil.executeWithLock("user_level_backfill_lock", 1, 300, () -> {
                int levelUpdated = userRepository.backfillLevels();
                int pointUpdated = userRepository.backfillPoints();
                log.info("사용자 백필 완료: level {}건, point {}건 갱신", levelUpdated, pointUpdated);
                return null;
            });
        } catch (Exception e) {
            log.error("backfillUserLevels() 작업 실패: {}", e.getMessage(), e);
        }
    }
}
