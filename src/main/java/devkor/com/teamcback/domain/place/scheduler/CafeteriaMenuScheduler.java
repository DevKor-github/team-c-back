package devkor.com.teamcback.domain.place.scheduler;

import devkor.com.teamcback.domain.place.service.CafeteriaMenuService;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Cafeteria Menu Scheduler")
@Component
@RequiredArgsConstructor
public class CafeteriaMenuScheduler {

    private final CafeteriaMenuService cafeteriaMenuService;
    private final RedisLockUtil redisLockUtil;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 10 0 * * *") // 매일 자정 10분마다
    public void updateMenus() {
        redisLockUtil.executeWithLock("menu_lock", 1, 300, () -> {

            System.out.println("--- 고려대학교 학식메뉴 스크래핑 시작 ---");

            // 수당삼양패컬티하우스 송림
            cafeteriaMenuService.scrapeMenu(503, 9757L);
            // 자연계 학생식당
            cafeteriaMenuService.scrapeMenu(504, 3103L);
            // 자연계 교직원 식당
            cafeteriaMenuService.scrapeMenu(504, 2490L);
            // 안암학사 식당
            cafeteriaMenuService.scrapeMenu(505, 3654L);
            // 산학관 식당
            cafeteriaMenuService.scrapeMenu(506, 3020L);
            // 교우회관 학생식당
            cafeteriaMenuService.scrapeMenu(507, 7705L);
            // 학생회관 학생식당
            cafeteriaMenuService.scrapeMenu(508, 9758L);

            System.out.println("------------------종료-------------------");
            return null;
        });

    }
}

