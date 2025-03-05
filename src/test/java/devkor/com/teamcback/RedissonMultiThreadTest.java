package devkor.com.teamcback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RedissonMultiThreadTest {

    @Autowired
    private RedissonClient redissonClient;

    private final String lockKey = "test_lock";

//    @Test
    public void testConcurrentLocking() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                RLock lock = redissonClient.getLock(lockKey);
                try {
                    if (lock.tryLock(1, 5, TimeUnit.SECONDS)) {
                        System.out.println("🔒 락 획득: " + Thread.currentThread().getName());
                        Thread.sleep(1000); // 1초 동안 락 유지
                    } else {
                        System.out.println("❌ 락 획득 실패: " + Thread.currentThread().getName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        System.out.println("🔓 락 해제: " + Thread.currentThread().getName());
                    }
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 종료될 때까지 대기
        executorService.shutdown();
    }
}
