package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "대표 캐릭터 장착 결과")
@Getter
public class EquipCharacterRes {
    @Schema(description = "장착한 캐릭터 ID", example = "1")
    private Long characterId;

    public EquipCharacterRes(Long characterId) {
        this.characterId = characterId;
    }
}
