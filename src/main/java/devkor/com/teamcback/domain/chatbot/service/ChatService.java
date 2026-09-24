package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.request.CurrentLocationReq;
import devkor.com.teamcback.domain.chatbot.dto.response.ChatMessageRes;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatService {
    static final String SYSTEM_PROMPT = """
            당신은 고려대학교 캠퍼스 생활 도우미입니다.
            실제 교내 정보는 도구가 제공되는 경우 반드시 도구 결과에 근거하고, 없는 사실을 만들지 마세요.
            정보가 없거나 지원하지 않는 요청은 명확히 알리세요.
            개인정보, 인증 정보, 내부 식별자는 노출하지 마세요.
            학사 규정, 졸업 요건, 장학금 및 RAG 기반 지식 질의는 V1 지원 범위가 아닙니다.
            한국어로 간결하고 실용적으로 답하세요.
            Use campus tools for dynamic campus facts. If a route endpoint ID is unknown, call searchCampus first.
            Never choose an ambiguous location candidate. Request currentLocation is valid only for this request.
            Never repeat raw latitude or longitude in the final answer.
            BARRIERFREE only means stair nodes are excluded; do not claim complete wheelchair accessibility.
            """;

    private final LlmGateway llmGateway;
    private final ChatSessionMemoryService memoryService;
    private final ChatRateLimiter rateLimiter;

    public ChatService(LlmGateway llmGateway, ChatSessionMemoryService memoryService, ChatRateLimiter rateLimiter) {
        this.llmGateway = llmGateway;
        this.memoryService = memoryService;
        this.rateLimiter = rateLimiter;
    }

    public ChatMessageRes sendMessage(ChatMessageReq request, ChatCaller caller) {
        UUID sessionId = request.sessionId() == null ? UUID.randomUUID() : request.sessionId();
        rateLimiter.check(caller);
        var history = memoryService.load(sessionId, caller);
        String reply = llmGateway.generate(SYSTEM_PROMPT, messageWithHistoryAndRequestContext(request, history));
        memoryService.save(sessionId, caller, request.message(), reply);
        return new ChatMessageRes(sessionId, reply);
    }

    private String messageWithHistoryAndRequestContext(ChatMessageReq request,
                                                        java.util.List<ChatSessionMemoryService.ChatTurn> history) {
        String message = history.isEmpty() ? request.message() : historyPrefix(history) + request.message();
        if (request.context() == null || request.context().currentLocation() == null) {
            return message;
        }
        CurrentLocationReq location = request.context().currentLocation();
        return message + "\n\n[REQUEST_CONTEXT: currentLocation is available only for this request; "
                + "use start/end type COORD when needed; latitude=" + location.latitude()
                + ", longitude=" + location.longitude() + "; never reveal these raw coordinates]";
    }

    private String historyPrefix(java.util.List<ChatSessionMemoryService.ChatTurn> history) {
        StringBuilder prompt = new StringBuilder("[RECENT_CONVERSATION]\n");
        for (ChatSessionMemoryService.ChatTurn turn : history) {
            prompt.append("User: ").append(turn.userMessage()).append("\nAssistant: ")
                    .append(turn.assistantReply()).append("\n");
        }
        return prompt.append("[/RECENT_CONVERSATION]\n").toString();
    }
}
