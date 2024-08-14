package devkor.com.teamcback.domain.operatingtime.sceduler;

import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.service.HolidayService;
import devkor.com.teamcback.domain.operatingtime.service.OperatingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Operating Time Scheduler")
@Component
@RequiredArgsConstructor
public class OperatingScheduler {
    private final OperatingService operatingService;
    private final HolidayService holidayService;

    private static final int SUMMER_VACATION_START_MONTH = 6;
    private static final int SUMMER_VACATION_START_DAY = 22;
    private static final int SUMMER_VACATION_END_MONTH = 9;
    private static final int SUMMER_VACATION_END_DAY = 1;

    private static final int WINTER_VACATION_START_MONTH = 12;
    private static final int WINTER_VACATION_START_DAY = 21;
    private static final int WINTER_VACATION_END_MONTH = 3;
    private static final int WINTER_VACATION_END_DAY = 3;

    private static DayOfWeek dayOfWeek = null;
    private static Boolean isHoliday = null;
    private static Boolean isVacation = null;
    private static Boolean isEvenWeek = null;

//    @Scheduled(cron = "0 * * * * *") // 테스트용
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정마다
    public void updateOperatingTime() {
        log.info("운영 시간 업데이트");
        LocalDate now = LocalDate.now();

        dayOfWeek = findDayOfWeek(now);
        log.info("dayOfWeek: {}", dayOfWeek.toString());
        isHoliday = isHoliday(now);
        log.info("isHoliday: {}", isHoliday);
        isVacation = isVacation(now);
        log.info("isVacation: {}", isVacation);

        isEvenWeek = false;
        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일이면 몇째주 토요일인지 계산
            isEvenWeek = isEvenWeek(now);
            log.info("isEvenWeek: {}", isEvenWeek);
        }

        operatingService.updateOperatingTime(dayOfWeek, isHoliday, isVacation, isEvenWeek);
    }

//    @Scheduled(cron = "*/20 * * * * *") // 테스트용
    @Scheduled(cron = "0 0,30 1-23 * * *") // 매일 30분마다 (자정 제외)
    public void updateIsOperating() {
        log.info("운영 여부 업데이트");
        LocalDateTime nowTime = LocalDateTime.now();

        operatingService.updateIsOperating(nowTime);
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

    private boolean isVacation(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // 여름방학 기간
        if((month == SUMMER_VACATION_START_MONTH && day >= SUMMER_VACATION_START_DAY) ||
            (month == SUMMER_VACATION_END_MONTH && day <= SUMMER_VACATION_END_DAY) ||
            (month > SUMMER_VACATION_START_MONTH && month < SUMMER_VACATION_END_MONTH)) {
            return true;
        }

        // 겨울방학 기간
        if((month == WINTER_VACATION_START_MONTH && day >= WINTER_VACATION_START_DAY) ||
            (month == WINTER_VACATION_END_MONTH && day <= WINTER_VACATION_END_DAY) ||
            (month < WINTER_VACATION_END_MONTH)) {
            return true;
        }

        return false;
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
