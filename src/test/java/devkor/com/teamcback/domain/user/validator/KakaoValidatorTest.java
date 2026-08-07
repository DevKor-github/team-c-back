package devkor.com.teamcback.domain.user.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import devkor.com.teamcback.domain.user.validator.client.KakaoClient;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.jwt.OIDC.OIDCUtil;
import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCDecodePayload;
import devkor.com.teamcback.global.redis.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KakaoValidatorTest {
    KakaoValidator kakaoValidator;

    @BeforeEach
    void setUp() {
        kakaoValidator = new KakaoValidator(
            Mockito.mock(OIDCUtil.class),
            Mockito.mock(KakaoClient.class),
            Mockito.mock(RedisUtil.class)
        );
    }

    @Test
    void keepsEmailFromVerifiedToken() {
        OIDCDecodePayload payload = new OIDCDecodePayload(
            "https://kauth.kakao.com",
            "client-id",
            "123456789",
            "member@example.com"
        );

        assertEquals("member@example.com", kakaoValidator.resolveEmail(payload));
    }

    @Test
    void createsStableInternalEmailWhenKakaoEmailIsMissing() {
        OIDCDecodePayload payload = new OIDCDecodePayload(
            "https://kauth.kakao.com",
            "client-id",
            "123456789",
            null
        );

        assertEquals(
            "kakao_123456789@noemail.kodaero.invalid",
            kakaoValidator.resolveEmail(payload)
        );
    }

    @Test
    void rejectsTokenWithoutEmailAndSubject() {
        OIDCDecodePayload payload = new OIDCDecodePayload(
            "https://kauth.kakao.com",
            "client-id",
            null,
            null
        );

        assertThrows(GlobalException.class, () -> kakaoValidator.resolveEmail(payload));
    }
}
