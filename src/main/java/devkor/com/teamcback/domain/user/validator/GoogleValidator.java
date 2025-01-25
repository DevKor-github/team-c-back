package devkor.com.teamcback.domain.user.validator;

import static devkor.com.teamcback.global.response.ResultCode.LOG_IN_REQUIRED;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_TOKEN;

import devkor.com.teamcback.domain.user.validator.client.GoogleClient;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.OIDCUtil;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeyDto;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import devkor.com.teamcback.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleValidator {
    private final OIDCUtil oidcUtil;
    private final GoogleClient googleClient;
    private final RedisUtil redisUtil;

    @Value("${jwt.social.google.iss}")
    private String ISS;
    @Value("${jwt.social.google.aud}")
    private String AUD;

    public OIDCPublicKeysResponse getCachedData() {
        return googleClient.getPublicKeys();
    }

    public String validateToken(String token) {
        try {
            // id_token 정보
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
            redisUtil.deleteCache("google::data");
            throw new GlobalException(e.getResultCode());
        } catch (Exception e) {
            throw new GlobalException(INVALID_TOKEN);
        }
    }
}
