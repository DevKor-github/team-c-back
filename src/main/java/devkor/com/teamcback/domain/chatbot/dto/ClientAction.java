package devkor.com.teamcback.domain.chatbot.dto;

public record ClientAction(
        ClientActionType type,
        NavigateRouteAction payload
) {
}
