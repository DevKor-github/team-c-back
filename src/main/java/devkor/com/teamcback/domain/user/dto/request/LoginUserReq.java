package devkor.com.teamcback.domain.user.dto.request;

import devkor.com.teamcback.domain.user.entity.Provider;
import lombok.Getter;

@Getter
public class LoginUserReq {
    private Provider provider;
    private String email;
    private String token;
}
