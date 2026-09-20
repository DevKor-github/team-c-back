package devkor.com.teamcback.domain.chatbot.dto.response;

import java.util.UUID;

public record ChatMessageRes(UUID sessionId, String reply) {
}
