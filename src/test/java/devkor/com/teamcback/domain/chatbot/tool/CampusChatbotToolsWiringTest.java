package devkor.com.teamcback.domain.chatbot.tool;

import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CampusChatbotToolsWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("chatbot.enabled=true")
            .withBean(CampusToolAdapter.class, () -> mock(CampusToolAdapter.class))
            .withBean(ChatbotToolCallLimiter.class, () -> mock(ChatbotToolCallLimiter.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void springCreatesBaseBeanAndBaseBeanCreatesRequestLocalTools() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CampusChatbotTools.class);

            CampusChatbotTools baseTools = context.getBean(CampusChatbotTools.class);
            CampusChatbotTools firstRequestTools = baseTools.forRequest(
                    new ResolvedLocationCollector(), mock(ChatbotToolCallLimiter.Scope.class));
            CampusChatbotTools secondRequestTools = baseTools.forRequest(
                    new ResolvedLocationCollector(), mock(ChatbotToolCallLimiter.Scope.class));

            assertThat(firstRequestTools).isNotSameAs(baseTools);
            assertThat(secondRequestTools).isNotSameAs(baseTools);
            assertThat(secondRequestTools).isNotSameAs(firstRequestTools);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(CampusChatbotTools.class)
    static class TestConfig {
    }
}
