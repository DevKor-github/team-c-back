package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatbotToolCallLimiter {
    private final ChatbotProperties properties;

    public ChatbotToolCallLimiter(ChatbotProperties properties) {
        this.properties = properties;
    }

    public Scope open() {
        return new Scope(new ScopeState(properties.agent().maxToolCallsPerMessage()));
    }

    public final class Scope implements AutoCloseable {
        private final ScopeState scope;

        private Scope(ScopeState scope) {
            this.scope = scope;
        }

        public int callCount() {
            return scope.callCount;
        }

        public void beforeToolCall() {
            scope.increment();
        }

        @Override
        public void close() {
        }
    }

    private static final class ScopeState {
        private final int maximum;
        private int callCount;

        private ScopeState(int maximum) {
            this.maximum = maximum;
        }

        private void increment() {
            if (callCount >= maximum) {
                throw new ToolCallLimitExceededException();
            }
            callCount++;
        }
    }
}
