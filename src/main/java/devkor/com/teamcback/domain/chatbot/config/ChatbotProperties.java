package devkor.com.teamcback.domain.chatbot.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(boolean enabled, Llm llm, Agent agent, Tools tools, RateLimit rateLimit) {
    public record Llm(String provider, String model, int maxOutputTokens, int timeoutSeconds) {
        public Duration timeout() {
            return Duration.ofSeconds(timeoutSeconds);
        }
    }

    public record Agent(int maxToolCallsPerMessage, int historyTurns, int sessionTtlMinutes,
                        int pendingRouteTtlMinutes) {
    }

    public record Tools(Limits search, Limits facilities, int menuMaxDays, Limits reviews) {
    }

    public record Limits(int defaultLimit, int maxLimit) {
    }

    public record RateLimit(int authenticatedDailyLimit, int anonymousDailyLimit, int burstPerMinute,
                            String dailyResetZone) {
    }
}
