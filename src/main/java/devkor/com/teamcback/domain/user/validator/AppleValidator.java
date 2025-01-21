package devkor.com.teamcback.domain.user.validator;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_TOKEN;
import static devkor.com.teamcback.global.response.ResultCode.LOG_IN_REQUIRED;

import devkor.com.teamcback.domain.user.validator.client.AppleClient;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.OIDCUtil;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeyDto;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import io.jsonwebtoken.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleValidator {
    private final OIDCUtil oidcUtil;
    private final AppleClient appleClient;
    private static final String KID = "kid";
    private static final String ALG = "alg";

    @Value("${jwt.social.apple.iss}")
    private String ISS;
    @Value("${jwt.social.apple.aud}")
    private String AUD;

    public OIDCPublicKeysResponse getCachedData() {
        return appleClient.getPublicKeys();
    }

    public String validateToken(String token) {
        try {
            // id_token 정보
            Header tokenInfo = oidcUtil.getUnsignedTokenClaims(token, AUD, ISS).getHeader();
            String kid = (String) tokenInfo.get(KID);
            String alg = (String) tokenInfo.get(ALG);

            // 공개키 가져오기
            OIDCPublicKeysResponse publicKeysResponse = getCachedData();

            OIDCPublicKeyDto oidcPublicKeyDto =
                publicKeysResponse.getKeys().stream()
                    .filter(o -> o.getKid().equals(kid) && o.getAlg().equals(alg))
                    .findFirst()
                    .orElseThrow(() -> new GlobalException(LOG_IN_REQUIRED));

            OIDCDecodePayload payload = oidcUtil.getOIDCTokenBody(token, oidcPublicKeyDto.getN(), oidcPublicKeyDto.getE());

            return payload.getSub();
        } catch(GlobalException e) {
            throw new GlobalException(LOG_IN_REQUIRED);
        } catch (Exception e) {
            throw new GlobalException(INVALID_TOKEN);
        }
    }
}
