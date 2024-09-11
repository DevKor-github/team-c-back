package devkor.com.teamcback.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginUserRes {
    String accessToken;
    String refreshToken;
}
