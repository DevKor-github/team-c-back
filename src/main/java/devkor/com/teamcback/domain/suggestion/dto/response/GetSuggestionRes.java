package devkor.com.teamcback.domain.suggestion.dto.response;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import lombok.Getter;

@Schema(description = "건의 조회")
@Getter
public class GetSuggestionRes {
    private Long id;
    private String title;
    private String type;
    private String content;
    private Long userId;
    private String createdAt;
    private boolean isSolved;

    public GetSuggestionRes(Suggestion suggestion) {
        this.id = suggestion.getId();
        this.title = suggestion.getTitle();
        this.type = suggestion.getSuggestionType().getType();
        this.content = suggestion.getContent();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.createdAt = suggestion.getCreatedAt().format(formatter);
        this.isSolved = suggestion.isSolved();
        this.userId = suggestion.getUser().getUserId();
    }
}
