package devkor.com.teamcback.domain.report.scheduler;

import devkor.com.teamcback.domain.report.service.ReportService;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Report status Scheduler")
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    @Value("${metrics.environment}")
    private String env;
    private final RedisLockUtil redisLockUtil;
    private final ReportService reportService;

    @Scheduled(cron = "0 1 0 * * *") // 매일 자정마다
    @EventListener(ApplicationReadyEvent.class)
    public void updateReportStatus() {

        // 배포 서버에서만 실행
        if(!env.equals("prod")) return;

        try{
            log.info("신고 상태 업데이트");

            redisLockUtil.executeWithLock("report-lock", 1, 300, () -> {
                reportService.updateExpiredReportStatus();
                return null;
            });
        } catch (Exception e) {
            log.info("updateReportStatus() 작업 실패: {}", e.getMessage(), e);
        }
    }
}
