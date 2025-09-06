package devkor.com.teamcback.domain.operatingtime.scheduler;

import devkor.com.teamcback.domain.SchoolCalendar.service.SchoolCalendarService;
import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.service.HolidayService;
import devkor.com.teamcback.domain.operatingtime.service.OperatingService;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Operating Time Scheduler")
@Component
@RequiredArgsConstructor
public class OperatingScheduler {
    private final OperatingService operatingService;
    private final HolidayService holidayService;
    private final RedisLockUtil redisLockUtil;
    private final SchoolCalendarService schoolCalendarService;

    private static DayOfWeek dayOfWeek = null;
    private static Boolean isHoliday = null;
    private static Boolean isVacation = null;
    private static Boolean isEvenWeek = null;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정마다
    @EventListener(ApplicationReadyEvent.class)
    public void updateOperatingTime() {
        setState();
        log.info("운영 시간 업데이트");

        redisLockUtil.executeWithLock("lock", 1, 300, () -> {
            operatingService.updateOperatingTime(dayOfWeek, isHoliday, isVacation, isEvenWeek);
            return null;
        });
    }


//    @Scheduled(cron = "30 35 * * * *") // 테스트용
    @Scheduled(cron = "0 */10 9-18 * * *") // 10분마다
    public void updateOperatingDuringPeakHour() {
        log.info("운영 여부 업데이트");
        redisLockUtil.executeWithLock("lock", 1, 300, () -> {
            operatingService.updateIsOperating(LocalTime.now(), dayOfWeek, isHoliday, isVacation, isEvenWeek);
            return null;
        });
    }

    @Scheduled(cron = "0 0,30 0-8,19-23 * * *") // 30분마다
    public void updateOperating() {
        log.info("운영 여부 업데이트");
        redisLockUtil.executeWithLock("lock", 1, 300, () -> {
            operatingService.updateIsOperating(LocalTime.now(), dayOfWeek, isHoliday, isVacation, isEvenWeek);
            return null;
        });
    }

    // 장소 운영 시간 저장 - 건물의 운영 시간에 변동이 있을 경우 1회 작동
    @EventListener(ApplicationReadyEvent.class)
    public void updatePlaceOperatingTime() {
        log.info("장소 운영 시간 업데이트");
        redisLockUtil.executeWithLock("lock", 1, 300, () -> {
            operatingService.updatePlaceOperatingTime();
            return null;
        });
    }

    private void setState() {
        LocalDate now = LocalDate.now();

        dayOfWeek = findDayOfWeek(now);
        log.info("dayOfWeek: {}", dayOfWeek.toString());
        isHoliday = isHoliday(now); // 공휴일 여부
        log.info("isHoliday: {}", isHoliday);
        isVacation = isVacation(); // 방학 여부
        log.info("isVacation: {}", isVacation);
        isEvenWeek = false; // 토요일 짝수 주 여부
        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일이면 몇째주 토요일인지 계산
            isEvenWeek = isEvenWeek(now);
            log.info("isEvenWeek: {}", isEvenWeek);
        }
    }

    private DayOfWeek findDayOfWeek(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case SATURDAY -> {
                return DayOfWeek.SATURDAY;
            }
            case SUNDAY -> {
                return DayOfWeek.SUNDAY;
            }
            default -> {
                return DayOfWeek.WEEKDAY;
            }
        }
    }

    private boolean isHoliday(LocalDate date) {
        return holidayService.isHoliday(date);
    }

    private boolean isVacation() {
        return schoolCalendarService.isVacationTf();
    }

    private boolean isEvenWeek(LocalDate now) {
        // 이번 달의 첫 번째 토요일 찾기
        LocalDate firstOfMonth = now.withDayOfMonth(1);
        LocalDate firstSaturday = firstOfMonth.with(TemporalAdjusters.nextOrSame(
            java.time.DayOfWeek.SATURDAY));

        int count = 0; // 주 차이
        LocalDate date = firstSaturday;
        while (date.isBefore(now) || date.isEqual(now)) {
            count++;
            date = date.plusWeeks(1);
        }
        log.info("count: {}", count);

        return count % 2 == 0;
    }
}
