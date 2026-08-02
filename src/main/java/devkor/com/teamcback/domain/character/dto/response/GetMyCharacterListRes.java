package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Schema(description = "보유 캐릭터 목록")
@Getter
public class GetMyCharacterListRes {
    @Schema(description = "보유 포인트", example = "25")
    private Long point;
    @Schema(description = "대표 장착 캐릭터 ID (미장착 시 null)", example = "1")
    private Long equippedCharacterId;
    @Schema(description = "보유 캐릭터 목록")
    private List<GetMyCharacterRes> characterList;

    public GetMyCharacterListRes(Long point, Long equippedCharacterId, List<GetMyCharacterRes> characterList) {
        this.point = point;
        this.equippedCharacterId = equippedCharacterId;
        this.characterList = characterList;
    }
}
