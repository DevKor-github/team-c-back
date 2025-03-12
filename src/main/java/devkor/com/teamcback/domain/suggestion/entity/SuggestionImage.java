package devkor.com.teamcback.domain.suggestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_suggestion_image")
@NoArgsConstructor
public class SuggestionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "suggestionId", nullable = false)
    private Suggestion suggestion;

    public SuggestionImage(String imageUrl, Suggestion suggestion) {
        this.imageUrl = imageUrl;
        this.suggestion = suggestion;
    }
}