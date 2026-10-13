package devkor.com.teamcback.domain.chatbot.dto.request;

import jakarta.validation.Valid;

public record ChatContextReq(@Valid CurrentLocationReq currentLocation) {
}
