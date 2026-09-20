package devkor.com.teamcback.domain.chatbot.controller;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.service.ChatService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.exception.handler.GlobalExceptionHandler;
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
    @Mock
    private LlmGateway llmGateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatService chatService = new ChatService(llmGateway);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsBlankMessageBeforeLlmCall() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(llmGateway);
    }

    @Test
    void rejectsMessageLongerThanOneThousandCharactersBeforeLlmCall() throws Exception {
        String request = "{\"message\":\"" + "a".repeat(1001) + "\"}";

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(llmGateway);
    }

    @Test
    void rejectsInvalidCoordinatesBeforeLlmCall() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "길을 알려줘",
                                  "context": {
                                    "currentLocation": {
                                      "latitude": 91.0,
                                      "longitude": -181.0
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(llmGateway);
    }

    @Test
    void generatesSessionIdAndReturnsGatewayReply() throws Exception {
        when(llmGateway.generate(anyString(), anyString())).thenReturn("안녕하세요.");

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  안녕  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data.sessionId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.reply").value("안녕하세요."));
    }

    @Test
    void hidesProviderFailureDetails() throws Exception {
        when(llmGateway.generate(anyString(), anyString()))
                .thenThrow(new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE));

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"안녕\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statusCode").value(20000))
                .andExpect(jsonPath("$.message").value("챗봇 서비스를 일시적으로 사용할 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
