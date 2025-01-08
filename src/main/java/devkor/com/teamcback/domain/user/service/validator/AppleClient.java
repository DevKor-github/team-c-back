package devkor.com.teamcback.domain.user.service.validator;

import devkor.com.teamcback.global.jwt.OIDC.dto.OIDCPublicKeysResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "appleClient", url = "https://appleid.apple.com")
public interface AppleClient {
    @Cacheable(value = "apple")
    @GetMapping("/auth/keys")
    OIDCPublicKeysResponse getPublicKeys();
}
