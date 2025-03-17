package devkor.com.teamcback.global.aop;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UpdateScoreAspect {

    private boolean needUpdate = true;
    private User user = null;
    private int addScore;

    private final UserRepository userRepository;
    private final UserBookmarkLogRepository userBookmarkLogRepository;

    @Before("@annotation(updateScore)")
    public void checkUpdatable(JoinPoint joinPoint, UpdateScore updateScore) {
        log.info("AOP 확인 절차 수행");

        // score 저장
        addScore = updateScore.addScore();

        // User 정보 찾기 (매개변수 검사로 찾기)
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        for (int i = 0; i < args.length; i++) {
            if (paramNames[i].equals("user") && args[i] instanceof User) {
                user = (User) args[i];
                break;
            } else if (paramNames[i].equals("userId") && args[i] instanceof Long userId) {
                user = userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
                break;
            }
        }

        // 유저 정보가 없으면 AOP 실행하지 않음
        if (user == null) {
            log.warn("user 또는 userId 부재");
            needUpdate = false;
            return;
        }

        // 유저 정보 로깅
        log.info("User : {}", user.getUserId());

        for (Object arg : args) {
            // 북마크 추가 시 점수 증가 여부 확인
            if (arg instanceof CreateBookmarkReq req) {
                if (userBookmarkLogRepository.existsByUserAndLocationIdAndLocationType(user, req.getLocationId(), req.getLocationType())) {
                    log.info("중복 Log: 점수가 증가하지 않습니다.");
                    needUpdate = false;
                    return;
                }
            }
        }
    }

    @AfterReturning(pointcut = "@annotation(devkor.com.teamcback.global.annotation.UpdateScore)")
    public void updateScore () {
        log.info("AOP 호출");

        // 점수 증가 필요한 경우에만 로직 실행
        if (needUpdate) increaseScore(user);
    }

    public void increaseScore(User user) {
        long newScore = user.getScore() + addScore;
        // 이전 레벨 계산
        Level beforeLv = getLevel(user.getScore());
        Level afterLv = getLevel(newScore);

        // 변했으면 true
        boolean isChanged = beforeLv != afterLv;
        if(isChanged) log.info("레벨 변경: {} -> {}", beforeLv.name(), afterLv.name());

        user.updateScore(newScore, isChanged);
        log.info("점수 증가: {} {}", user.getUserId(), newScore);
    }

    private Level getLevel(Long score) {
        // score >= minScore 인 경우 중 가장 높은 레벨 반환
        return Arrays.stream(Level.values())
            .filter(lv -> score >= lv.getMinScore())
            .max(Comparator.comparingInt(Level::getMinScore))
            .orElse(Level.LEVEL1);
    }
}
