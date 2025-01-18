package devkor.com.teamcback.domain.user.dto.request;

import devkor.com.teamcback.domain.user.entity.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "자동 로그인에 필요한 사용자 정보")
@Getter
public class BypassLoginReq {
    @Schema(description = "소셜 종류", example = "KAKAO")
    private Provider provider;
    @Schema(description = "이메일 혹은 sub", example = "leeyejin113@gmail.com")
    private String email;
    @Schema(description = "개인 식별 코드", example = "123e4567-e89b-12d3-a456-426614174000")
    private String code;
}
