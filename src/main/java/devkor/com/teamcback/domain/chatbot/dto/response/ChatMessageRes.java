package devkor.com.teamcback.domain.chatbot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.chatbot.dto.ClientAction;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageRes(UUID sessionId, String reply, ClientAction action) {
    public ChatMessageRes(UUID sessionId, String reply) {
        this(sessionId, reply, null);
    }
}
