package devkor.com.teamcback.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "자동 로그인에 필요한 사용자 정보")
@Getter
public class AutoLoginReq {
    @Schema(description = "이메일", example = "leeyejin113@gmail.com")
    private String email;
    @Schema(description = "개인 식별 코드", example = "123e4567-e89b-12d3-a456-426614174000")
    private String code;
}
