package devkor.com.teamcback.domain.user.service;

import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.dto.response.ModifyUsernameRes;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // profile 이미지 링크
    @Value("${profile.image.lv1-url}")
    private String lv1Url;
    @Value("${profile.image.lv2-url}")
    private String lv2Url;
    @Value("${profile.image.lv3-url}")
    private String lv3Url;
    @Value("${profile.image.lv4-url}")
    private String lv4Url;
    @Value("${profile.image.lv5-url}")
    private String lv5Url;

    /**
     * 마이페이지 정보 조회
     */
    public GetUserInfoRes getUserInfo(Long userId) {
        User user = findUser(userId);
        Level level = getLevel(user.getScore());

        return new GetUserInfoRes(user, categoryRepository.countAllByUser(user), level, getProfileUrl(level));
    }

    /**
     * 사용자 별명 수정
     */
    @Transactional
    public ModifyUsernameRes modifyUsername(Long userId, String username) {
        User user = findUser(userId);

        // 유저명 중목 확인
        checkDuplicatedUsername(user, username);

        user.update(username);

        return new ModifyUsernameRes();
    }

    private void checkDuplicatedUsername(User user, String username) {
        User usingUser = userRepository.findByUsername(username);

        if(usingUser != null) {
            // 본인이 현재 사용 중인 닉네임과 동일한 경우
            if(usingUser.equals(user)) {
                throw new GlobalException(USERNAME_IN_USE);
            }
            // 다른 사용자가 해당 별명을 사용 중인 경우
            throw new GlobalException(DUPLICATED_USERNAME);
        }
    }

    private Level getLevel(Long score) {
        // score >= minScore 인 경우 중 가장 높은 레벨 반환
        return Arrays.stream(Level.values())
            .filter(level -> score >= level.getMinScore())
            .max(Comparator.comparingInt(Level::getMinScore))
            .orElse(Level.LEVEL1);
    }

    private String getProfileUrl(Level level) {
        //레벨에 맞는 profile 이미지 반환
        return switch (level) {
            case LEVEL1 -> lv1Url;
            case LEVEL2 -> lv2Url;
            case LEVEL3 -> lv3Url;
            case LEVEL4 -> lv4Url;
            case LEVEL5 -> lv5Url;
        };
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }
}
