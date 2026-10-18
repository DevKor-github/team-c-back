package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.service.SurveyPushScheduleWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "push.survey", name = "worker-enabled", havingValue = "true")
public class SurveyPushWorkerScheduler {
    private final PushIdleWorkerRunner runner;
    private final SurveyPushScheduleWorker worker;

    @Scheduled(fixedDelayString = "${push.survey.poll-interval-ms:60000}")
    public void processDueSchedules() {
        runner.run(PushWorkSignals.Work.SURVEY, worker::processDueSchedulesOnce);
    }
}
