package devkor.com.teamcback.domain.suggestion.service;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.dto.response.CreateSuggestionRes;
import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestionService {
    private final SuggestionRepository suggestionRepository;

    /**
     * 건의 생성
     */
    @Transactional
    public CreateSuggestionRes createSuggestion(CreateSuggestionReq req) {
        Suggestion suggestion = new Suggestion(req);

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);

        return new CreateSuggestionRes(savedSuggestion);
    }
}
