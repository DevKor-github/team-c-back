package devkor.com.teamcback.domain.chatbot.controller;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_RATE_LIMITED;
import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import devkor.com.teamcback.domain.chatbot.service.ChatOrchestrationResult;
import devkor.com.teamcback.domain.chatbot.service.ChatOrchestrator;
import devkor.com.teamcback.domain.chatbot.service.ChatRateLimiter;
import devkor.com.teamcback.domain.chatbot.service.ChatService;
import devkor.com.teamcback.domain.chatbot.service.ChatSessionMemoryService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.exception.handler.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
    @Mock private ChatOrchestrator chatOrchestrator;
    @Mock private ChatSessionMemoryService memoryService;
    @Mock private ChatRateLimiter rateLimiter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatService chatService = new ChatService(chatOrchestrator, memoryService, rateLimiter);
        lenient().when(memoryService.load(any(), any())).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void rejectsBlankMessageBeforeLlmCall() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chatOrchestrator);
    }

    @Test
    void rejectsMessageLongerThanOneThousandCharactersBeforeLlmCall() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chatOrchestrator);
    }

    @Test
    void rejectsInvalidCoordinatesBeforeLlmCall() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"route\",\"context\":{\"currentLocation\":{"
                                + "\"latitude\":91.0,\"longitude\":-181.0}}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chatOrchestrator);
    }

    @Test
    void generatesSessionIdAndReturnsGatewayReplyWithoutAction() throws Exception {
        when(chatOrchestrator.executeGeneral(anyString(), anyList(), anyString()))
                .thenReturn(new ChatOrchestrationResult("hello", List.of()));
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data.sessionId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.reply").value("hello"))
                .andExpect(jsonPath("$.data.action").doesNotExist());
    }

    @Test
    void returns429BeforeProviderCallWhenRateLimited() throws Exception {
        doThrow(new GlobalException(CHATBOT_RATE_LIMITED)).when(rateLimiter).check(any());
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.statusCode").value(20001));
        verifyNoInteractions(chatOrchestrator);
    }

    @Test
    void hidesProviderFailureDetails() throws Exception {
        when(chatOrchestrator.executeGeneral(anyString(), anyList(), anyString()))
                .thenThrow(new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE));
        mockMvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statusCode").value(20000))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
