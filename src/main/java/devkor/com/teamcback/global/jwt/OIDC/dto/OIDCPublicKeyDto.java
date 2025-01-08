package devkor.com.teamcback.global.jwt.OIDC.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OIDCPublicKeyDto {
    private String kid;
    private String alg;
    private String use;
    private String n;
    private String e;
}
