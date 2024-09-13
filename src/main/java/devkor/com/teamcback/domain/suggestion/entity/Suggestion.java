package devkor.com.teamcback.domain.suggestion.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.*;
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

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean isSolved = false;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    public Suggestion(CreateSuggestionReq req, User user) {
        this.title = req.getTitle();
        this.suggestionType = req.getType();
        this.content = req.getContent();
        this.user = user;
    }

    public void updateIsSolved(boolean solved) {
        this.isSolved = solved;
    }
}
