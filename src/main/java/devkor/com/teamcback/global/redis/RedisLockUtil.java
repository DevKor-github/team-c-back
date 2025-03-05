package devkor.com.teamcback.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final RedissonClient redissonClient;

    /**
     * 분산 락을 활용하여 특정 작업 실행
     *
     * @param lockKey   락 키 (ex: "scheduler_lock")
     * @param waitTime  락을 기다리는 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     * @param task      실행할 작업 (람다식으로 전달)
     */
    public <T> void executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
                try {
                    task.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("다른 서버에서 실행 중이므로 종료");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

