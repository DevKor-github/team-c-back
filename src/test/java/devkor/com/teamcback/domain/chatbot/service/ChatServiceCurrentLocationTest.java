package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.request.ChatContextReq;
import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.request.CurrentLocationReq;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceCurrentLocationTest {
    @Mock LlmGateway llmGateway;
    @Mock ChatSessionMemoryService memoryService;
    @Mock ChatRateLimiter rateLimiter;

    @Test
    void passesCurrentLocationOnlyInCurrentProviderRequestAndNeverSavesIt() {
        when(llmGateway.generate(anyString(), anyString())).thenReturn("경로 안내");
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        service.sendMessage(new ChatMessageReq(sessionId, "현재 위치에서 중도까지 가줘",
                new ChatContextReq(new CurrentLocationReq(37.5861, 127.0290))), caller);
        service.sendMessage(new ChatMessageReq(sessionId, "다시 알려줘", null), caller);

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(llmGateway, org.mockito.Mockito.times(2)).generate(anyString(), messages.capture());
        assertThat(messages.getAllValues().get(0))
                .contains("현재 위치에서 중도까지 가줘", "latitude=37.5861", "longitude=127.029");
        assertThat(messages.getAllValues().get(1)).isEqualTo("다시 알려줘")
                .doesNotContain("37.5861", "127.029", "currentLocation");
        verify(memoryService).save(sessionId, caller, "현재 위치에서 중도까지 가줘", "경로 안내");
        verify(memoryService).save(sessionId, caller, "다시 알려줘", "경로 안내");
    }
}
