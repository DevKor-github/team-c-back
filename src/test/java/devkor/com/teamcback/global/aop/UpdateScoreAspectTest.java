package devkor.com.teamcback.global.aop;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.service.BookmarkService;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.entity.SuggestionType;
import devkor.com.teamcback.domain.suggestion.service.SuggestionService;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.service.UserService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

@Slf4j
@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UpdateScoreAspectTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private BookmarkService bookmarkService;

    @Test
    public void suggestionScoreTest() {
        Long userId = 36L; // 임시로 제 계정 사용했습니다.
        User user = findUser(userId);
        CreateSuggestionReq req = new CreateSuggestionReq("Test제목", SuggestionType.INCONVENIENCE, "내용", "이메일");

        // 건의함 작성 시 포인트 확인
        long beforeScore = user.getScore();
        log.info("건의함 작성 전 Score : " + beforeScore);
        suggestionService.createSuggestion(userId, req, null);

        user = findUser(userId);
        long afterScore = user.getScore();
        log.info("건의함 작성 후 Score : " + afterScore);
        Assertions.assertThat(beforeScore+3).isEqualTo(afterScore);

        // 점수가 기준치를 넘었는지 확인
        if(user.isUpgraded()) {
            log.info("레벨이 올랐습니다.");

            // 업그레이드 후 마이페이지 첫 조회
            GetUserInfoRes info = userService.getUserInfo(userId);
            log.info("마이페이지 isUgraded : " + info.isUpgraded());
            Assertions.assertThat(info.isUpgraded()).isTrue(); // True

            user = findUser(userId); // 조회 후 User 정보
            log.info("조회 후 isUpgraded : " + user.isUpgraded());
            Assertions.assertThat(user.isUpgraded()).isFalse(); // false
        } else {
            log.info("레벨이 오르지 않았습니다.");
        }
    }

    @Test
    public void bookmarkScoreTest() {
        Long userId = 36L; // 임시로 제 계정 사용했습니다.
        List<Long> list = new ArrayList<>();

        list.add(96L); // 카테고리 ID : 새 로그 검사용
        LocationType type = LocationType.PLACE;
        long placeId = 60L;
        String memo = "memo";

        User user = findUser(userId);
        CreateBookmarkReq req = new CreateBookmarkReq(list, type, placeId, memo);

        // 1. 건의함 작성 시 포인트 확인
        long beforeScore = user.getScore();
        log.info("북마크 생성 전 Score : " + beforeScore);
        bookmarkService.createBookmark(userId, req);

        user = findUser(userId);
        long afterScore = user.getScore();
        log.info("북마크 생성 후 Score : " + afterScore);
        Assertions.assertThat(beforeScore+1).isEqualTo(afterScore);

        // 2. 점수가 기준치를 넘었는지 확인
        if(user.isUpgraded()) {
            log.info("레벨이 올랐습니다.");

            // 업그레이드 후 마이페이지 첫 조회
            GetUserInfoRes info = userService.getUserInfo(userId);
            log.info("마이페이지 isUgraded : " + info.isUpgraded());
            Assertions.assertThat(info.isUpgraded()).isTrue(); // True

            user = findUser(userId); // 조회 후 User 정보
            log.info("조회 후 isUpgraded : " + user.isUpgraded());
            Assertions.assertThat(user.isUpgraded()).isFalse(); // false
        } else {
            log.info("레벨이 오르지 않았습니다.");
        }

        // 3. 다른 카테고리에 동일 장소 북마크 추가 : 점수 증가X
        log.info("북마크 재생성 전 Score : " + afterScore);
        list.clear();
        list.add(99L);
        req = new CreateBookmarkReq(list, type, placeId, memo);
        bookmarkService.createBookmark(userId, req);

        user = findUser(userId);
        long againScore = user.getScore();
        log.info("북마크 재생성 후 Score : " + againScore);
        Assertions.assertThat(afterScore).isEqualTo(againScore);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }
}
