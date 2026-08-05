package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.service.PushMessageRecoveryWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PushMessageRecoverySchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(PushMessageRecoveryWorker.class, () -> mock(PushMessageRecoveryWorker.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void schedulerIsDisabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(PushMessageRecoveryScheduler.class));
    }

    @Test
    void schedulerIsDisabledWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("push.recovery-worker.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(PushMessageRecoveryScheduler.class));
    }

    @Configuration
    @Import(PushMessageRecoveryScheduler.class)
    static class TestConfig {
    }
}
