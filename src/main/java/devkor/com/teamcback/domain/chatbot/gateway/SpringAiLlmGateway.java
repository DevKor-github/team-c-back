package devkor.com.teamcback.domain.chatbot.gateway;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ToolCallLimitExceededException;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class SpringAiLlmGateway implements LlmGateway {
    private static final String TOOL_LIMIT_FALLBACK = "요청을 처리하는 데 필요한 확인이 너무 많습니다. "
            + "장소나 조건을 조금 더 구체적으로 알려 주세요.";
    private final ChatClient chatClient;
    private final ChatbotProperties properties;
    private final ExecutorService chatbotLlmExecutor;
    private final CampusChatbotTools campusChatbotTools;
    private final ChatbotToolCallLimiter toolCallLimiter;

    public SpringAiLlmGateway(ChatClient.Builder chatClientBuilder, ChatbotProperties properties,
                              ExecutorService chatbotLlmExecutor, CampusChatbotTools campusChatbotTools,
                              ChatbotToolCallLimiter toolCallLimiter) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.chatbotLlmExecutor = chatbotLlmExecutor;
        this.campusChatbotTools = campusChatbotTools;
        this.toolCallLimiter = toolCallLimiter;
    }

    @Override
    public String generate(String systemPrompt, String userMessage) {
        Future<GatewayResult> response = chatbotLlmExecutor.submit(() -> invoke(systemPrompt, userMessage));
        try {
            GatewayResult result = response.get(properties.llm().timeout().toMillis(), TimeUnit.MILLISECONDS);
            log.info("chatbot_llm outcome={} provider={} model={} latencyMs={} toolCalls={} inputTokens=unavailable outputTokens=unavailable",
                    result.outcome(), properties.llm().provider(), properties.llm().model(), result.latencyMillis(),
                    result.toolCalls());
            return result.content();
        } catch (InterruptedException exception) {
            response.cancel(true);
            Thread.currentThread().interrupt();
            log.warn("chatbot_llm outcome=TEMPORARILY_UNAVAILABLE provider={} model={}",
                    properties.llm().provider(), properties.llm().model());
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        } catch (ExecutionException | TimeoutException exception) {
            response.cancel(true);
            if (hasCause(exception, ToolCallLimitExceededException.class)) {
                log.info("chatbot_llm outcome=TOOL_LIMIT provider={} model={}",
                        properties.llm().provider(), properties.llm().model());
                return TOOL_LIMIT_FALLBACK;
            }
            log.warn("chatbot_llm outcome=TEMPORARILY_UNAVAILABLE provider={} model={}",
                    properties.llm().provider(), properties.llm().model());
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    private GatewayResult invoke(String systemPrompt, String userMessage) {
        long startedAt = System.nanoTime();
        try (ChatbotToolCallLimiter.Scope scope = toolCallLimiter.open()) {
            try {
                String content = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .tools(campusChatbotTools)
                        .call()
                        .content();
                if (content == null || content.isBlank()) {
                    throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
                }
                return new GatewayResult(content, "SUCCESS", elapsedMillis(startedAt), scope.callCount());
            } catch (RuntimeException exception) {
                if (hasCause(exception, ToolCallLimitExceededException.class)) {
                    return new GatewayResult(TOOL_LIMIT_FALLBACK, "TOOL_LIMIT", elapsedMillis(startedAt),
                            scope.callCount());
                }
                throw exception;
            }
        }
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record GatewayResult(String content, String outcome, long latencyMillis, int toolCalls) {
    }
}
