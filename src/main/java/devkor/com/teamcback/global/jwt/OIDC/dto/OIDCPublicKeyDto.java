package devkor.com.teamcback.global.jwt.OIDC.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OIDCPublicKeyDto implements Serializable {
    private String kid;
    private String alg;
    private String use;
    private String n;
    private String e;
}
