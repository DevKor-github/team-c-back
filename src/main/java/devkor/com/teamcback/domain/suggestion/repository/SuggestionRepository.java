package devkor.com.teamcback.domain.suggestion.repository;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    Page<Suggestion> findByIsSolved(Pageable pageable, Boolean isSolved);

    Page<Suggestion> findBySuggestionType(Pageable pageable, SuggestionType type);

    Page<Suggestion> findBySuggestionTypeAndIsSolved(Pageable pageable, SuggestionType type, Boolean isSolved);
}
