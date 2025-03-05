package devkor.com.teamcback.domain.operatingtime.scheduler;

import devkor.com.teamcback.domain.operatingtime.service.HolidayService;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Holiday Scheduler")
@Component
@RequiredArgsConstructor
public class HolidayScheduler {
    private final HolidayService holidayService;
    private final RedisLockUtil redisLockUtil;

//    @Scheduled(cron = "0 * * * * *") // 테스트용
    @Scheduled(cron = "0 0 0 1 * ?") // 매달 1일 자정마다
    public void updateHoliday() {
        redisLockUtil.executeWithLock("lock", 1, 300, () -> {
            LocalDateTime nowTime = LocalDateTime.now();
            try {
                holidayService.updateHolidays(nowTime.getYear(), nowTime.getMonthValue());
            } catch (URISyntaxException e) {
                log.info("휴일 저장 스케줄러 실패");
            }
            return null;
        });
    }
}
