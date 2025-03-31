package devkor.com.teamcback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RedissonLockTest {

    @Autowired
    private RedissonClient redissonClient;

//    @Test
    public void testLockAcquisition() throws InterruptedException {
        String lockKey = "test_lock";
        RLock lock = redissonClient.getLock(lockKey);

        // 락을 획득할 수 있는지 확인
        boolean isLocked = lock.tryLock(1, 5, TimeUnit.SECONDS);
        assertThat(isLocked).isTrue(); // 정상적으로 락을 획득해야 함

        // 락 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

