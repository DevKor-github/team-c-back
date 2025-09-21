package devkor.com.teamcback.global.aop;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.vote.dto.request.SaveVoteRecordReq;
import devkor.com.teamcback.domain.vote.repository.VoteRecordRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

    private final UserRepository userRepository;
    private final UserBookmarkLogRepository userBookmarkLogRepository;
    private final VoteRecordRepository voteRecordRepository;

    @Around("@annotation(updateScore)")
    public Object updateScore(ProceedingJoinPoint joinPoint, UpdateScore updateScore) throws Throwable {
        int addScore = updateScore.addScore();

        Object[] args = joinPoint.getArgs(); // 변수값
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames(); // 변수명

        // User 정보 찾기
        User user = getUser(args, paramNames);

        // 점수 갱신 불가 확인
        if(!checkUpdatable(user, args)) {
            return joinPoint.proceed();
        }

        // 비지니스 로직 수행
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.info("비지니스 로직에서 예외가 발생했습니다.");
            throw e; // 기존 흐름 유지
        }

        // 점수 증가
        increaseScore(user, addScore);

        return result;
    }

    private User getUser(Object[] args, String[] paramNames) {
        User user = null;
        for (int i = 0; i < args.length; i++) {
            if (paramNames[i].equals("user") && args[i] instanceof User) {
                user = (User) args[i];
                break;
            } else if (paramNames[i].equals("userId") && args[i] instanceof Long userId) {
                user = userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
                break;
            }
        }
        return user;
    }

    private boolean checkUpdatable(User user, Object[] args) {
        // 유저 정보가 없으면 AOP 실행하지 않음
        if (user == null) {
            log.warn("User 정보를 찾을 수 없습니다.");
            return false;
        }

        // 기타 조건 확인
        for (Object arg : args) {
            // 북마크 추가 시 점수 증가 여부 확인 (중복 로그 확인)
            if (arg instanceof CreateBookmarkReq req) {
                if (userBookmarkLogRepository.existsByUserAndLocationIdAndLocationType(user, req.getLocationId(), req.getLocationType())) {
                    return false;
                }
            }

            if (arg instanceof SaveVoteRecordReq req) {
                if (voteRecordRepository.existsByUserIdAndVoteId(user.getUserId(), req.getVoteId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public void increaseScore(User user, int addScore) {
        long newScore = user.getScore() + addScore;
        // 전후 레벨 계산
        Level beforeLv = getLevel(user.getScore());
        Level afterLv = getLevel(newScore);

        // 변했으면 true
        boolean isChanged = beforeLv != afterLv;
        user.updateScore(newScore, isChanged);
    }

    private Level getLevel(Long score) {
        // score >= minScore 인 경우 중 가장 높은 레벨 반환
        return Arrays.stream(Level.values())
            .filter(lv -> score >= lv.getMinScore())
            .max(Comparator.comparingInt(Level::getMinScore))
            .orElse(Level.LEVEL1);
    }
}
