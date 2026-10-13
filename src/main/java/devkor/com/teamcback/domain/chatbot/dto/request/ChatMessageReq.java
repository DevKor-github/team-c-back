package devkor.com.teamcback.domain.chatbot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChatMessageReq(
        UUID sessionId,
        @NotBlank @Size(max = 1000) String message,
        @Valid ChatContextReq context
) {
    public ChatMessageReq {
        if (message != null) {
            message = message.trim();
        }
    }
}
