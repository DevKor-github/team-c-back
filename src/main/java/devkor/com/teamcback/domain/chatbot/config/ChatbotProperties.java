package devkor.com.teamcback.domain.chatbot.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(boolean enabled, Llm llm, Agent agent, Tools tools) {
    public record Llm(String provider, String model, int maxOutputTokens, int timeoutSeconds) {
        public Duration timeout() {
            return Duration.ofSeconds(timeoutSeconds);
        }
    }

    public record Agent(int maxToolCallsPerMessage, int historyTurns, int sessionTtlMinutes) {
    }

    public record Tools(Limits search, Limits facilities) {
    }

    public record Limits(int defaultLimit, int maxLimit) {
    }
}
