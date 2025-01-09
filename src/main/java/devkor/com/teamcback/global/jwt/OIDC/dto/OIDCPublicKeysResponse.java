package devkor.com.teamcback.global.jwt.OIDC.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OIDCPublicKeysResponse implements Serializable {
    List<OIDCPublicKeyDto> keys;
}
