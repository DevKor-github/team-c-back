package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.request.ChatContextReq;
import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.request.CurrentLocationReq;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceCurrentLocationTest {
    @Mock LlmGateway llmGateway;

    @Test
    void passesCurrentLocationOnlyInCurrentProviderRequestWithoutAddingMemoryState() {
        when(llmGateway.generate(anyString(), anyString())).thenReturn("경로 안내");
        ChatService service = new ChatService(llmGateway);
        UUID sessionId = UUID.randomUUID();

        service.sendMessage(new ChatMessageReq(sessionId, "현재 위치에서 중도까지 가줘",
                new ChatContextReq(new CurrentLocationReq(37.5861, 127.0290))));
        service.sendMessage(new ChatMessageReq(sessionId, "다시 알려줘", null));

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(llmGateway, org.mockito.Mockito.times(2)).generate(anyString(), messages.capture());
        assertThat(messages.getAllValues().get(0))
                .contains("현재 위치에서 중도까지 가줘", "latitude=37.5861", "longitude=127.029");
        assertThat(messages.getAllValues().get(1)).isEqualTo("다시 알려줘")
                .doesNotContain("37.5861", "127.029", "currentLocation");
        assertThat(ChatService.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .doesNotContain("currentLocation", "latitude", "longitude", "memory");
    }
}
