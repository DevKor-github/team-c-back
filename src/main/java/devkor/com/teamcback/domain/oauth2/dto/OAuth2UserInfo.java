package devkor.com.teamcback.domain.oauth2.dto;

import static devkor.com.teamcback.global.response.ResultCode.ILLEGAL_REGISTRATION_ID;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.global.exception.GlobalException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2UserInfo {

    private String oauthId;
    private String email;
    private Provider provider;

    @Builder
    private OAuth2UserInfo(String oauthId, String email, Provider provider) {
        this.oauthId = oauthId;
        this.email = email;
        this.provider = provider;
    }

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new GlobalException(ILLEGAL_REGISTRATION_ID);
        };
    }

    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
            .oauthId((String) attributes.get("sub"))
            .email((String) attributes.get("email"))
            .provider(Provider.GOOGLE)
            .build();
    }

    private static OAuth2UserInfo ofKakao(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        return OAuth2UserInfo.builder()
            .oauthId((String) profile.get("nickname"))
            .email((String) account.get("email"))
            .provider(Provider.KAKAO)
            .build();
    }

    private static OAuth2UserInfo ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuth2UserInfo.builder()
            .oauthId((String) response.get("id"))
            .email((String) response.get("email"))
            .provider(Provider.NAVER)
            .build();
    }
}
