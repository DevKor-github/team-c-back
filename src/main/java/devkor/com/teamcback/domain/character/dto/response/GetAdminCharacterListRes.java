package devkor.com.teamcback.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Schema(description = "관리자용 캐릭터 목록")
@Getter
public class GetAdminCharacterListRes {
    @Schema(description = "캐릭터 목록")
    private List<GetAdminCharacterRes> characterList;

    public GetAdminCharacterListRes(List<GetAdminCharacterRes> characterList) {
        this.characterList = characterList;
    }
}
