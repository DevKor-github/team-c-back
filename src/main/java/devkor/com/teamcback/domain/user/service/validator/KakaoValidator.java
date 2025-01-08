package devkor.com.teamcback.domain.user.service.validator;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.OIDCUtil;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeyDto;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoValidator{
    private final OIDCUtil oidcUtil;
    private final KakaoClient kakaoClient;

    @Value("${jwt.social.kakao.iss}")
    private String ISS;
    @Value("${jwt.social.kakao.aud}")
    private String AUD;

    public OIDCPublicKeysResponse getCachedData() {
        return kakaoClient.getPublicKeys();
    }

    public String validateToken(String token) {
        try {
            // 카카오 id_token 정보
            String kid = oidcUtil.getKidFromUnsignedTokenHeader(token, AUD, ISS);

            // 공개키 가져오기
            OIDCPublicKeysResponse publicKeysResponse = getCachedData();

            OIDCPublicKeyDto oidcPublicKeyDto =
                publicKeysResponse.getKeys().stream()
                    .filter(o -> o.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow();

            OIDCDecodePayload payload = oidcUtil.getOIDCTokenBody(token, oidcPublicKeyDto.getN(), oidcPublicKeyDto.getE());

            return payload.getEmail();
        } catch (Exception e) {
            throw new GlobalException(UNAUTHORIZED);
        }
    }
}
