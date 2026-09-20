package devkor.com.teamcback.domain.chatbot.gateway;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.CampusChatbotTools;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class SpringAiLlmGateway implements LlmGateway {
    private final ChatClient chatClient;
    private final ChatbotProperties properties;
    private final ExecutorService chatbotLlmExecutor;
    private final CampusChatbotTools campusChatbotTools;

    public SpringAiLlmGateway(ChatClient.Builder chatClientBuilder, ChatbotProperties properties,
                              ExecutorService chatbotLlmExecutor, CampusChatbotTools campusChatbotTools) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.chatbotLlmExecutor = chatbotLlmExecutor;
        this.campusChatbotTools = campusChatbotTools;
    }

    @Override
    public String generate(String systemPrompt, String userMessage) {
        Future<String> response = chatbotLlmExecutor.submit(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .tools(campusChatbotTools)
                .call()
                .content());
        try {
            String content = response.get(properties.llm().timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (content == null || content.isBlank()) {
                throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
            }
            return content;
        } catch (InterruptedException exception) {
            response.cancel(true);
            Thread.currentThread().interrupt();
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        } catch (ExecutionException | TimeoutException exception) {
            response.cancel(true);
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }
}
