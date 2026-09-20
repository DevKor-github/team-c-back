package devkor.com.teamcback.domain.chatbot.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService chatbotLlmExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "chatbot-llm-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
