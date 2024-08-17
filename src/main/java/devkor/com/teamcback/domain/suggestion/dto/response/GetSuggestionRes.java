package devkor.com.teamcback.domain.suggestion.dto.response;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "건의 조회")
@Getter
public class GetSuggestionRes {
    private Long id;
    private String title;
    private String type;
    private String content;
    private String createdAt;

    public GetSuggestionRes(Suggestion suggestion) {
        this.id = suggestion.getId();
        this.title = suggestion.getTitle();
        this.type = suggestion.getSuggestionType().getType();
        this.content = suggestion.getContent();
        this.createdAt = suggestion.getCreatedAt().toString();
    }
}
