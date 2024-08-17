package devkor.com.teamcback.domain.suggestion.repository;

import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

}
