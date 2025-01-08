package devkor.com.teamcback.global.jwt.OIDC.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OIDCPublicKeysResponse {
    List<OIDCPublicKeyDto> keys;
}
