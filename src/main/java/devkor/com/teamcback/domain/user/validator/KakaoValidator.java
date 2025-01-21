package devkor.com.teamcback.domain.user.validator;

import static devkor.com.teamcback.global.response.ResultCode.LOG_IN_REQUIRED;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_TOKEN;

import devkor.com.teamcback.domain.user.validator.client.KakaoClient;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.OIDCUtil;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeyDto;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
                    .orElseThrow(() -> new GlobalException(LOG_IN_REQUIRED));

            OIDCDecodePayload payload = oidcUtil.getOIDCTokenBody(token, oidcPublicKeyDto.getN(), oidcPublicKeyDto.getE());

            return payload.getEmail();
        } catch(GlobalException e) {
            throw new GlobalException(LOG_IN_REQUIRED);
        } catch (Exception e) {
            throw new GlobalException(INVALID_TOKEN);
        }
    }
}
