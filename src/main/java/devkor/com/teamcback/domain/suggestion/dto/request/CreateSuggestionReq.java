package devkor.com.teamcback.domain.suggestion.dto.request;

import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Getter;

@Schema(description = "건의 제목, 분류, 내용")
@Getter
public class CreateSuggestionReq {
    @Schema(description = "제목", example = "장소 위치가 잘못됨")
    private String title;
    @Schema(description = "분류", example = "LOCATION_ERROR")
    private SuggestionType type;
    @Schema(description = "내용", example = "장소 위치 변동")
    private String content;
    @Schema(description = "이메일", example = "lee@gmail.com")
    @Nullable
    private String email;
}
