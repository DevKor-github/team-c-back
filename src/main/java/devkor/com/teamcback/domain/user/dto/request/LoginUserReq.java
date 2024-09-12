package devkor.com.teamcback.domain.user.dto.request;

import devkor.com.teamcback.domain.user.entity.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "회원가입 및 로그인에 필요한 사용자 정보")
@Getter
public class LoginUserReq {
    @Schema(description = "소셜 종류", example = "KAKAO")
    private Provider provider;
    @Schema(description = "사용자 email", example = "leeyejin113@gmail.com")
    private String email;
    @Schema(description = "소셜 검증용 정보")
    private String token;
}
