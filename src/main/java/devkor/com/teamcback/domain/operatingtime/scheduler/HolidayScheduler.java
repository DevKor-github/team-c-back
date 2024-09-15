package devkor.com.teamcback.domain.operatingtime.scheduler;

import devkor.com.teamcback.domain.operatingtime.service.HolidayService;
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

//    @Scheduled(cron = "0 * * * * *") // 테스트용
    @Scheduled(cron = "0 0 0 1 * ?") // 매달 1일 자정마다
    public void updateHoliday() throws URISyntaxException {
        LocalDateTime nowTime = LocalDateTime.now();
        int year = nowTime.getYear();
        int month = nowTime.getMonthValue();

        holidayService.updateHolidays(year, month);
    }
}
