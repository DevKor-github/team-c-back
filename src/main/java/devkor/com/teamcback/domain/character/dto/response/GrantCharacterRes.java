package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "캐릭터 수동 지급 결과")
@Getter
public class GrantCharacterRes {
    @Schema(description = "지급된 사용자 캐릭터 ID", example = "1")
    private Long userCharacterId;

    public GrantCharacterRes(Long userCharacterId) {
        this.userCharacterId = userCharacterId;
    }
}
