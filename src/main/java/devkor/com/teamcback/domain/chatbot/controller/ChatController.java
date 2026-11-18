package devkor.com.teamcback.domain.chatbot.controller;

import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.response.ChatMessageRes;
import devkor.com.teamcback.domain.chatbot.service.ChatService;
import devkor.com.teamcback.domain.chatbot.service.ChatCaller;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/messages")
    public CommonResponse<ChatMessageRes> sendMessage(@Valid @RequestBody ChatMessageReq request,
                                                       @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                       HttpServletRequest httpRequest) {
        return CommonResponse.success(chatService.sendMessage(request,
                ChatCaller.from(userDetails, httpRequest.getRemoteAddr())));
    }
}
