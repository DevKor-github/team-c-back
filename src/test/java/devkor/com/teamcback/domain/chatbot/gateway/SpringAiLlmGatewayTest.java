package devkor.com.teamcback.domain.chatbot.gateway;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.CampusChatbotTools;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiLlmGatewayTest {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void mapsProviderFailureWithoutExposingProviderDetails() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().system("system").user("hello").call().content())
                .thenThrow(new IllegalStateException("provider secret and internal details"));
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60),
                new ChatbotProperties.Tools(
                        new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        CampusChatbotTools tools = mock(CampusChatbotTools.class);
        SpringAiLlmGateway gateway = new SpringAiLlmGateway(builder, properties, executor, tools,
                new devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter(properties));

        assertThatThrownBy(() -> gateway.generate("system", "hello"))
                .isInstanceOfSatisfying(GlobalException.class, exception -> {
                    assertThat(exception.getResultCode()).isEqualTo(CHATBOT_TEMPORARILY_UNAVAILABLE);
                    assertThat(exception.getMessage()).isNull();
                    assertThat(exception.getCause()).isNull();
                });
    }
}
