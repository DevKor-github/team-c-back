package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Schema(description = "스토어 정보 (보유 포인트 + 캐릭터 목록)")
@Getter
public class GetStoreRes {
    @Schema(description = "보유 포인트", example = "25")
    private Long point;
    @Schema(description = "캐릭터 목록")
    private List<GetStoreCharacterRes> characterList;

    public GetStoreRes(Long point, List<GetStoreCharacterRes> characterList) {
        this.point = point;
        this.characterList = characterList;
    }
}
