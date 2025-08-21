package devkor.com.teamcback.domain.suggestion.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_SUGGESTION;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

import devkor.com.teamcback.domain.common.util.EmailUtil;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.dto.response.CreateSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.GetSuggestionRes;
import devkor.com.teamcback.domain.suggestion.dto.response.ModifySuggestionRes;
import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionImage;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import devkor.com.teamcback.infra.s3.S3Util;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionService {
    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final S3Util s3Util;
    private final EmailUtil emailUtil;

    /**
     * 건의 생성
     */
    @Transactional
    @UpdateScore(addScore = 3)
    public CreateSuggestionRes createSuggestion(Long userId, CreateSuggestionReq req, List<MultipartFile> images) {
        User user = null;
        if(userId != null) user = findUser(userId);
        Suggestion suggestion = new Suggestion(req, user);

        List<SuggestionImage> suggestionImages = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                String imageUrl = s3Util.uploadFile(image, FilePath.SUGGESTION);
                suggestionImages.add(new SuggestionImage(imageUrl, suggestion));
            }
        }

        suggestion.setImages(suggestionImages);

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);

        // 이메일 전송
        try {
            // 이메일 전송
            emailUtil.sendNotificationMessage(savedSuggestion.getTitle(),
                    user == null ? "익명" : user.getUsername(),
                    savedSuggestion.getSuggestionType().getType(),
                    savedSuggestion.getContent(),
                    savedSuggestion.getImages());
        } catch (MessagingException e) {
            log.error("이메일 전송 실패: {}", e.getMessage());
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
