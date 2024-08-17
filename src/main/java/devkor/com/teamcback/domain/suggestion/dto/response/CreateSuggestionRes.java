package devkor.com.teamcback.domain.suggestion.dto.response;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "건의 생성 완료")
@Getter
public class CreateSuggestionRes {
    private Long suggestionId;
    public CreateSuggestionRes(Suggestion suggestion) {
        this.suggestionId = suggestion.getId();
    }
}
