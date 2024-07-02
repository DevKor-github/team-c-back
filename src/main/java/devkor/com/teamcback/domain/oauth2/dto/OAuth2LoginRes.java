package devkor.com.teamcback.domain.oauth2.dto;

import lombok.Getter;

@Getter
public class OAuth2LoginRes {
    private String accessToken;
    private String refreshToken;

    public OAuth2LoginRes(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
