package devkor.com.teamcback.global.jwt.OIDC;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OIDCUtil {
    private final RestTemplate restTemplate;
    private static final String KID = "kid";
    public String getKidFromUnsignedTokenHeader(String token, String aud, String iss) {
        return (String) getUnsignedTokenClaims(token, aud, iss).getHeader().get(KID);
    }

    public Jwt<Header, Claims> getUnsignedTokenClaims(String token, String aud, String iss) {
        try {
            return Jwts.parserBuilder()
                .requireAudience(aud)
                .requireIssuer(iss)
                .build()
                .parseClaimsJwt(getUnsignedToken(token));
        } catch (Exception e) {
            throw new GlobalException(UNAUTHORIZED);
        }
    }

    private String getUnsignedToken(String token) {
        String[] splitToken = token.split("\\.");
        if (splitToken.length != 3) throw new GlobalException(UNAUTHORIZED);
        return splitToken[0] + "." + splitToken[1] + ".";
    }

    public OIDCPublicKeysResponse getPublicKeys(String url) throws Exception {
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response, OIDCPublicKeysResponse.class);
    }

    public OIDCDecodePayload getOIDCTokenBody(String token, String modulus, String exponent) {
        Claims body = getOIDCTokenJws(token, modulus, exponent).getBody();
        return new OIDCDecodePayload(
            body.getIssuer(),
            body.getAudience(),
            body.getSubject(),
            body.get("email", String.class));
    }

    private Jws<Claims> getOIDCTokenJws(String token, String modulus, String exponent) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getRSAPublicKey(modulus, exponent))
                .build()
                .parseClaimsJws(token);
        } catch (Exception e) {
            throw new GlobalException(UNAUTHORIZED);
        }
    }

    private Key getRSAPublicKey(String modulus, String exponent) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] decodeN = Base64.getUrlDecoder().decode(modulus);
        byte[] decodeE = Base64.getUrlDecoder().decode(exponent);
        BigInteger n = new BigInteger(1, decodeN);
        BigInteger e = new BigInteger(1, decodeE);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
        return keyFactory.generatePublic(keySpec);
    }
}
