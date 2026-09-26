package devkor.com.teamcback.domain.chatbot.config;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Keeps Vertex function responses JSON-shaped when a tool callback fails. */
@Configuration
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatbotToolExecutionConfig {
    @Bean
    ToolExecutionExceptionProcessor chatbotToolExecutionExceptionProcessor() {
        return new ToolExecutionExceptionProcessor() {
            @Override
            public String process(ToolExecutionException exception) {
                return "{\"success\":false,\"error\":\"TOOL_EXECUTION_FAILED\"}";
            }
        };
    }
}
