package devkor.com.teamcback.global.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {
    public static final String ACCESS_TOKEN_HEADER = "AccessToken"; // Access Token Key 값
    public static final String REFRESH_TOKEN_HEADER = "RefreshToken"; // Refresh Token Key 값
    private static final String AUTHORIZATION_KEY = "auth"; // 사용자 권한 값의 Key
    private static final String BEARER_PREFIX = "Bearer%20"; // Token 식별자
    private static final long ACCESS_TOKEN_TIME = 60 * 60 * 1000L; // Access Token 만료시간 1시간
    private static final long REFRESH_TOKEN_TIME = 14 * 24 * 60 * 60 * 1000L; // Refresh Token 만료시간 2주

    @Value("${jwt.secret.key}")
    private String secretKey; // JWT 생성 및 검증에 사용할 비밀키 (Base 64로 Encode)
    private Key key; // JWT 생성 및 검증에 사용할 Key 인터페이스를 구현한 객체
    private JwtParser jwtParser; // JWT 검증을 위한 JwtParser 객체 (유효성 검사 및 Payload 추출)
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey); // secretKey를 Base64 디코딩하여 byte 배열로 저장
        key = Keys.hmacShaKeyFor(bytes); // HMAC SHA 알고리즘으로 키를 생성
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build(); // key로 JwtParser 초기화
    }

    /**
     * Access Token 생성
     * @param email 사용자 email
     * @param role 사용자 권한(USER, ADMIN)
     * @return "bearer%20" + Access Token
     */
    public String createAccessToken(String email, String role) {
        Date now = new Date();

        return BEARER_PREFIX + Jwts.builder()
            .setSubject(email)
            .claim(AUTHORIZATION_KEY, role)
            .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_TIME))
            .setIssuedAt(now)
            .signWith(key, signatureAlgorithm)
            .compact();
    }

    /**
     * Refresh Token 생성
     * @return "bearer%20 + Refresh Token
     */
    public String createRefreshToken(String email, String role) {
        Date now = new Date();

        return BEARER_PREFIX + Jwts.builder()
            .setSubject(email)
            .claim(AUTHORIZATION_KEY, role)
            .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_TIME))
            .setIssuedAt(now)
            .signWith(key, signatureAlgorithm)
            .compact();
    }

    /**
     * Cookie 에서 Access Token 가져오기
     * @return Access Token 혹은 null
     */
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return findValueInCookie(request, ACCESS_TOKEN_HEADER);
    }

    /**
     * Cookie 에서 Refresh Token 가져오기
     * @return Refresh Token 혹은 null
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return findValueInCookie(request, REFRESH_TOKEN_HEADER);
    }

    private String findValueInCookie(HttpServletRequest request, String key) {
        Cookie[] list = request.getCookies();
        if(list != null) {
            for (Cookie cookie : list) {
                if (cookie.getName().equals(key))
                    return cookie.getValue().substring(9);
            }
        }
        return null;
    }

    /**
     * 토큰 검증
     * @return VALID(유효함), EXPIRED(만료됨), INVALID(유효하지 않음)
     */
    public TokenStatus validateToken(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return TokenStatus.INVALID;
    }

    /**
     * 토큰에서 사용자 email 가져오기
     * @return email
     */
    public String getUserEmailFromToken (String token) {
        return jwtParser.parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * 토큰에서 사용자 role 가져오기
     * @return (String) role
     */
    public String getRoleFromToken (String token) {
        return (String) jwtParser.parseClaimsJws(token).getBody().get(AUTHORIZATION_KEY);
    }

    /**
     * 토큰의 남은 유효 시간 확인
     * @return 남은 유효 시간(분)
     */
    public int getExpiration(String token) {
        Date expiration = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getExpiration();

        long now = new Date().getTime();

        return (int) (expiration.getTime() - now) / (60 * 1000);
    }
}
