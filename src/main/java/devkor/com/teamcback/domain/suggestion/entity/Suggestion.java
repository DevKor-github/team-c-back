package devkor.com.teamcback.domain.suggestion.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_suggestion")
@NoArgsConstructor
public class Suggestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SuggestionType suggestionType;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isSolved = false;

    // TODO: 로그인 개발 이후 User와 다대일 연관관계

    public Suggestion(CreateSuggestionReq req) {
        this.title = req.getTitle();
        this.suggestionType = req.getType();
        this.content = req.getContent();
    }

    public void updateIsSolved(boolean solved) {
        this.isSolved = solved;
    }
}
