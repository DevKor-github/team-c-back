package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChatServiceArchitectureTest {
    @Test
    void delegatesLlmWorkflowThroughChatOrchestrator() {
        Field[] fields = ChatService.class.getDeclaredFields();

        assertThat(Arrays.stream(fields).map(Field::getType))
                .contains(ChatOrchestrator.class)
                .noneMatch(type -> type.getName().startsWith("org.springframework.ai")
                        || type.getName().startsWith("com.google"));

        assertThat(Arrays.stream(ChatOrchestrator.class.getDeclaredFields()).map(Field::getType))
                .contains(LlmGateway.class)
                .noneMatch(type -> type.getName().startsWith("org.springframework.ai")
                        || type.getName().startsWith("com.google"));
    }
}
