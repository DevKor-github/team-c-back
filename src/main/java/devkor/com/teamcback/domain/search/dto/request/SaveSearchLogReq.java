package devkor.com.teamcback.domain.search.dto.request;

import devkor.com.teamcback.domain.common.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "검색결과 id, 이름, type")
@Getter
public class SaveSearchLogReq {
    @Schema(description = "저장할 검색결과 시설 id", example = "5")
    private Long id;
    @Schema(description = "저장할 검색결과 시설 이름", example = "우정정보관 201호")
    private String name;
    @Schema(description = "저장할 검색결과 시설 종류", example = "CLASSROOM")
    private PlaceType type;
}
