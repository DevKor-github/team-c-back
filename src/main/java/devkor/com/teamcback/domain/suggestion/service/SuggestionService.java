package devkor.com.teamcback.domain.suggestion.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_SUGGESTION;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.dto.response.CreateSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.GetSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.ModifySuggestionRes;
import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
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
    private final UserRepository userRepository;

    /**
     * 건의 생성
     */
    @Transactional
    public CreateSuggestionRes createSuggestion(Long userId, CreateSuggestionReq req) {
        User user = null;
        if(userId != null) user = findUser(userId);
        Suggestion suggestion = new Suggestion(req, user);

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);

        //건의 생성 시 score 증가
        if(user != null) {
            user.updateScore(user.getScore() + 3);
        }

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

    @Transactional
    public ModifySuggestionRes modifySuggestions(Long suggestionId, boolean isSolved) {
        Suggestion suggestion = findSuggestion(suggestionId);

        suggestion.updateIsSolved(isSolved);

        return new ModifySuggestionRes();
    }

    private Suggestion findSuggestion(Long id) {
        return suggestionRepository.findById(id).orElseThrow(() ->new GlobalException(NOT_FOUND_SUGGESTION));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }
}
