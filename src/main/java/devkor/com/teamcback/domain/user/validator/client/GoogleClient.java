package devkor.com.teamcback.domain.user.validator.client;

import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "googleClient", url = "https://www.googleapis.com")
public interface GoogleClient {
    @Cacheable(value = "google", key = "'data'")
    @GetMapping("/oauth2/v3/certs")
    OIDCPublicKeysResponse getPublicKeys();
}
