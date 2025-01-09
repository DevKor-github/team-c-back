package devkor.com.teamcback.domain.user.validator.client;

import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "kakaoClient", url = "https://kauth.kakao.com")
public interface KakaoClient {

    @Cacheable(value = "kakao", key = "'data'")
    @GetMapping("/.well-known/jwks.json")
    OIDCPublicKeysResponse getPublicKeys();
}

