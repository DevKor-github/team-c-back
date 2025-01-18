package devkor.com.teamcback.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BypassLoginRes {
    String accessToken;
    String refreshToken;
}
