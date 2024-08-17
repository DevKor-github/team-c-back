package devkor.com.teamcback.domain.suggestion.service;

import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.dto.response.CreateSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.GetSuggestionRes;
import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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

    /**
     * 건의 조회
     */
    @Transactional(readOnly = true)
    public Page<GetSuggestionRes> getSuggestions(int page, int size, String sortBy, boolean isAsc, SuggestionType type, Boolean isSolved) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (type == null && isSolved == null) {
            return suggestionRepository.findAll(pageable).map(GetSuggestionRes::new);
        }
        else if (type == null) {
            return suggestionRepository.findByIsSolved(pageable, isSolved).map(GetSuggestionRes::new);
        }
        else if (isSolved == null) {
            return suggestionRepository.findBySuggestionType(pageable, type).map(GetSuggestionRes::new);
        }
        else {
            return suggestionRepository.findBySuggestionTypeAndIsSolved(pageable, type, isSolved).map(GetSuggestionRes::new);
        }
    }
}