package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock ChatOrchestrator chatOrchestrator;
    @Mock ChatSessionMemoryService memoryService;
    @Mock ChatRateLimiter rateLimiter;

    @Test
    void passesCurrentLocationOnlyInCurrentProviderRequestAndNeverSavesIt() {
        when(chatOrchestrator.execute(anyString(), anyList(), anyString()))
                .thenReturn(new ChatOrchestrationResult("route answer", List.of()));
        ChatService service = new ChatService(chatOrchestrator, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        service.sendMessage(new ChatMessageReq(sessionId, "route from current location",
                new ChatContextReq(new CurrentLocationReq(37.5861, 127.0290))), caller);
        service.sendMessage(new ChatMessageReq(sessionId, "tell me again", null), caller);

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(chatOrchestrator, org.mockito.Mockito.times(2)).execute(anyString(), anyList(), messages.capture());
        assertThat(messages.getAllValues().get(0)).contains("latitude=37.5861", "longitude=127.029");
        assertThat(messages.getAllValues().get(1)).isEqualTo("tell me again")
                .doesNotContain("37.5861", "127.029", "currentLocation");
        verify(memoryService).save(sessionId, caller, "route from current location", "route answer");
        verify(memoryService).save(sessionId, caller, "tell me again", "route answer");
    }

    @Test
    void preservesRecentRolesAndKeepsCurrentCorrectionAsLatestUserMessage() {
        when(chatOrchestrator.execute(anyString(), anyList(), anyString()))
                .thenReturn(new ChatOrchestrationResult("answer", List.of()));
        ChatService service = new ChatService(chatOrchestrator, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of(
                new ChatSessionMemoryService.ChatTurn("old user", "old assistant")));

        service.sendMessage(new ChatMessageReq(sessionId, "current correction", null), caller);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmGateway.ConversationMessage>> history = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> currentMessage = ArgumentCaptor.forClass(String.class);
        verify(chatOrchestrator).execute(anyString(), history.capture(), currentMessage.capture());
        assertThat(history.getValue()).containsExactly(
                new LlmGateway.ConversationMessage(LlmGateway.Role.USER, "old user"),
                new LlmGateway.ConversationMessage(LlmGateway.Role.ASSISTANT, "old assistant"));
        assertThat(currentMessage.getValue()).isEqualTo("current correction");
    }
}
