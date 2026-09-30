package devkor.com.teamcback.domain.chatbot.tool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import devkor.com.teamcback.domain.chatbot.service.ToolCallLimitExceededException;
import org.junit.jupiter.api.Test;

class CampusChatbotToolsToolLimitTest {
    @Test
    void seventhActualToolMethodDoesNotReachAdapter() {
        CampusToolAdapter adapter = mock(CampusToolAdapter.class);
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "model", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60, 15),
                new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7, new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        ChatbotToolCallLimiter limiter = new ChatbotToolCallLimiter(properties);
        try (ChatbotToolCallLimiter.Scope scope = limiter.open()) {
            CampusChatbotTools tools = new CampusChatbotTools(adapter, limiter)
                    .forRequest(new ResolvedLocationCollector(), scope);
            for (int index = 0; index < 6; index++) {
                tools.getCampusStatus();
            }
            assertThatThrownBy(tools::getCampusStatus).isInstanceOf(ToolCallLimitExceededException.class);
        }
        verify(adapter, times(6)).getCampusStatus();
    }
}
