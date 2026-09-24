package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatbotToolCallLimiter {
    private final ChatbotProperties properties;
    private final ThreadLocal<ScopeState> current = new ThreadLocal<>();

    public ChatbotToolCallLimiter(ChatbotProperties properties) {
        this.properties = properties;
    }

    public Scope open() {
        ScopeState previous = current.get();
        ScopeState scope = new ScopeState(properties.agent().maxToolCallsPerMessage());
        current.set(scope);
        return new Scope(scope, previous);
    }

    public void beforeToolCall() {
        ScopeState scope = current.get();
        if (scope != null) {
            scope.increment();
        }
    }

    public final class Scope implements AutoCloseable {
        private final ScopeState scope;
        private final ScopeState previous;

        private Scope(ScopeState scope, ScopeState previous) {
            this.scope = scope;
            this.previous = previous;
        }

        public int callCount() {
            return scope.callCount;
        }

        @Override
        public void close() {
            if (previous == null) {
                current.remove();
            } else {
                current.set(previous);
            }
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
