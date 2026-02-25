package devkor.com.teamcback.global.aop;

import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.review.dto.request.CreateReviewReq;
import devkor.com.teamcback.domain.review.dto.request.ModifyReviewReq;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.suggestion.dto.request.CreateSuggestionReq;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.vote.dto.request.SaveVoteRecordReq;
import devkor.com.teamcback.domain.vote.repository.VoteRecordRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UpdateScoreAspect {

    private final UserRepository userRepository;
    private final SuggestionRepository suggestionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final FileRepository fileRepository;

    // 리뷰 점수 상수
    private static final int REVIEW_BASE_SCORE = 3;      // 기본 점수 (별점)
    private static final int REVIEW_COMMENT_SCORE = 7;   // 한줄평 추가 점수
    private static final int REVIEW_IMAGE_SCORE = 3;     // 사진 추가 점수
    private static final int REVIEW_COMMENT_MIN_LENGTH = 10;  // 한줄평 최소 글자 수

    @Around("@annotation(updateScore)")
    public Object updateScore(ProceedingJoinPoint joinPoint, UpdateScore updateScore) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        String methodName = joinPoint.getSignature().getName();

        // User 정보 찾기
        User user = getUser(args, paramNames);

        // 동적 점수 계산 모드
        if (updateScore.dynamic()) {
            return handleDynamicScore(joinPoint, user, args, paramNames, methodName);
        }

        // 기존 고정 점수 모드
        return handleFixedScore(joinPoint, updateScore.addScore(), user, args);
    }

    /**
     * 기존 고정 점수 처리 (Vote, Suggestion 등)
     */
    private Object handleFixedScore(ProceedingJoinPoint joinPoint, int addScore, User user, Object[] args) throws Throwable {
        // 점수 갱신 불가 확인
        if (!checkUpdatable(user, args)) {
            Object result = joinPoint.proceed();
            injectScoreInfo(result, user, false, false);
            return result;
        }

        // 비지니스 로직 수행
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.info("비지니스 로직에서 예외가 발생했습니다.");
            throw e;
        }

        // 점수 증가
        increaseScore(user, addScore);

        // 점수 정보 주입
        injectScoreInfo(result, user, user.isUpgraded(), true);

        return result;
    }

    /**
     * 동적 점수 처리 (Review 등)
     */
    private Object handleDynamicScore(ProceedingJoinPoint joinPoint, User user, Object[] args, String[] paramNames, String methodName) throws Throwable {
        if (user == null) {
            log.warn("User 정보를 찾을 수 없습니다.");
            Object result = joinPoint.proceed();
            injectScoreInfo(result, null, false, false);
            return result;
        }

        // 리뷰 생성
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof CreateReviewReq req) {
                return handleReviewCreate(joinPoint, user, args, paramNames, req);
            }
        }

        // 리뷰 수정
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ModifyReviewReq req) {
                Long reviewId = getReviewId(args, paramNames);
                return handleReviewModify(joinPoint, user, reviewId, req);
            }
        }

        // 리뷰 삭제 (메서드 이름으로 판단)
        if (methodName.equals("deleteReview")) {
            Long reviewId = getReviewId(args, paramNames);
            return handleReviewDelete(joinPoint, user, reviewId);
        }

        // 기타 동적 점수 케이스 (확장용)
        Object result = joinPoint.proceed();
        injectScoreInfo(result, user, false, false);
        return result;
    }

    /**
     * 리뷰 생성 시 점수 처리
     */
    private Object handleReviewCreate(ProceedingJoinPoint joinPoint, User user, Object[] args, String[] paramNames, CreateReviewReq req) throws Throwable {
        // placeId 추출
        Long placeId = getPlaceId(args, paramNames);
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));

        // 중복 체크: 같은 장소에 오늘 이미 리뷰 작성했는지
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        if (reviewRepository.existsByUserAndPlaceAndCreatedAtBetween(user, place, startOfDay, endOfDay)) {
            throw new GlobalException(ALREADY_REVIEWED_TODAY);
        }

        // 비즈니스 로직 수행
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.info("비지니스 로직에서 예외가 발생했습니다.");
            throw e;
        }

        // 동적 점수 계산
        int addScore = calculateReviewScore(req.getComment(), req.getImages() != null && !req.getImages().isEmpty());

        // 점수 증가
        increaseScore(user, addScore);

        // 점수 정보 주입
        injectScoreInfo(result, user, user.isUpgraded(), true);

        return result;
    }

    /**
     * 리뷰 수정 시 점수 처리
     */
    private Object handleReviewModify(ProceedingJoinPoint joinPoint, User user, Long reviewId, ModifyReviewReq req) throws Throwable {
        // 기존 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_REVIEW));

        // 기존 점수 계산
        boolean hadImages = fileRepository.existsByFileUuid(review.getFileUuid());
        int oldScore = calculateReviewScore(review.getComment(), hadImages);

        // 비즈니스 로직 수행
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.info("비지니스 로직에서 예외가 발생했습니다.");
            throw e;
        }

        // 새 점수 계산
        int newScore = calculateReviewScore(req.getComment(), req.getImages() != null && !req.getImages().isEmpty());

        // 점수 차이 계산 및 적용
        int scoreDiff = newScore - oldScore;
        if (scoreDiff != 0) {
            updateScoreWithDiff(user, scoreDiff);
            injectScoreInfo(result, user, user.isUpgraded(), scoreDiff > 0);
        } else {
            injectScoreInfo(result, user, false, false);
        }

        return result;
    }

    /**
     * 리뷰 삭제 시 점수 처리
     */
    private Object handleReviewDelete(ProceedingJoinPoint joinPoint, User user, Long reviewId) throws Throwable {
        // 삭제 전 리뷰 조회 (삭제 후에는 조회 불가)
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_REVIEW));

        // 기존 점수 계산
        boolean hadImages = fileRepository.existsByFileUuid(review.getFileUuid());
        int scoreToDeduct = calculateReviewScore(review.getComment(), hadImages);

        // 비즈니스 로직 수행 (리뷰 삭제)
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.info("비지니스 로직에서 예외가 발생했습니다.");
            throw e;
        }

        // 점수 감소
        updateScoreWithDiff(user, -scoreToDeduct);

        // 점수 정보 주입 (삭제 시 scoreGained는 항상 false)
        injectScoreInfo(result, user, user.isUpgraded(), false);

        return result;
    }

    /**
     * 리뷰 점수 계산
     */
    private int calculateReviewScore(String comment, boolean hasImages) {
        int score = REVIEW_BASE_SCORE;  // 기본 점수 (별점)

        // 한줄평 점수 (10글자 이상)
        if (comment != null && comment.length() >= REVIEW_COMMENT_MIN_LENGTH) {
            score += REVIEW_COMMENT_SCORE;
        }

        // 사진 점수
        if (hasImages) {
            score += REVIEW_IMAGE_SCORE;
        }

        return score;
    }

    /**
     * 파라미터에서 placeId 추출
     */
    private Long getPlaceId(Object[] args, String[] paramNames) {
        for (int i = 0; i < args.length; i++) {
            if (paramNames[i].equals("placeId") && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        throw new GlobalException(NOT_FOUND_PLACE);
    }

    /**
     * 파라미터에서 reviewId 추출
     */
    private Long getReviewId(Object[] args, String[] paramNames) {
        for (int i = 0; i < args.length; i++) {
            if (paramNames[i].equals("reviewId") && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        throw new GlobalException(NOT_FOUND_REVIEW);
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
            // 건의 작성 시 하루 2회까지만 점수 지급
            if (arg instanceof CreateSuggestionReq) {
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                long todayCount = suggestionRepository.countByUserAndCreatedAtBetween(user, startOfDay, endOfDay);
                if (todayCount >= 2) {
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

    private void injectScoreInfo(Object result, User user, boolean isLevelUp, boolean scoreGained) {
        if (result instanceof ScoreUpdateResponse response && user != null) {
            response.setLevelUp(isLevelUp);
            response.setCurrentScore(user.getScore());
            response.setScoreGained(scoreGained);
        }
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

    /**
     * 점수 차이만큼 업데이트 (증가 또는 감소)
     */
    private void updateScoreWithDiff(User user, int scoreDiff) {
        long newScore = Math.max(0, user.getScore() + scoreDiff);  // 최소 0점

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
