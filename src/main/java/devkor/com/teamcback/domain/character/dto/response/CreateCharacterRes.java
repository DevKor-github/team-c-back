package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "캐릭터 생성 결과")
@Getter
public class CreateCharacterRes {
    @Schema(description = "생성된 캐릭터 ID", example = "1")
    private Long characterId;

    public CreateCharacterRes(Long characterId) {
        this.characterId = characterId;
    }
}
