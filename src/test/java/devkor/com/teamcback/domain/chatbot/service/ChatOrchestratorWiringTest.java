package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ChatOrchestratorWiringTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("chatbot.enabled=true")
            .withBean(LlmGateway.class, () -> mock(LlmGateway.class))
            .withBean(CampusToolAdapter.class, () -> mock(CampusToolAdapter.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void createsOrchestratorWithPlannerThroughAutowiredConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ChatOrchestrator.class);
            assertThat(context).hasSingleBean(RoutePlanner.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ChatOrchestrator.class, RoutePlanner.class})
    static class TestConfig {
    }
}
