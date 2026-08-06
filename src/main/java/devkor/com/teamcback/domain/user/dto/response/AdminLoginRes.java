package devkor.com.teamcback.domain.user.dto.response;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.User;
import lombok.Getter;

@Getter
public class AdminLoginRes {
    private final String accessToken;
    private final String refreshToken;
    private final Long userId;
    private final String username;
    private final String email;
    private final Provider provider;

    public AdminLoginRes(String accessToken, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.provider = user.getProvider();
    }
}
