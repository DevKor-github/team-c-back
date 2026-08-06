package devkor.com.teamcback.domain.user.dto.request;

import devkor.com.teamcback.domain.user.entity.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "관리자 소셜 로그인 요청")
public class AdminLoginReq {
    @Schema(description = "관리자 소셜 로그인 제공자", allowableValues = {"GOOGLE", "KAKAO"})
    private Provider provider;

    @Schema(description = "소셜 로그인에서 발급받은 OIDC ID Token")
    private String token;
}
