package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.global.security.UserDetailsImpl;

public record ChatCaller(String key, boolean authenticated) {
    public static ChatCaller from(UserDetailsImpl userDetails, String remoteAddress) {
        if (userDetails != null && userDetails.getUser() != null && userDetails.getUser().getUserId() != null) {
            return new ChatCaller("user:" + userDetails.getUser().getUserId(), true);
        }
        String safeAddress = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
        return new ChatCaller("ip:" + safeAddress, false);
    }
}
