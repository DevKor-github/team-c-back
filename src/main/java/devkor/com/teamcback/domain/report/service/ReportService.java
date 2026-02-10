package devkor.com.teamcback.domain.report.service;

import devkor.com.teamcback.domain.report.dto.request.CreateReviewReportReq;
import devkor.com.teamcback.domain.report.dto.response.CreateReviewReportRes;
import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.report.repository.ReportRepository;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static devkor.com.teamcback.domain.report.entity.ReportStatus.PENDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_REVIEW;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateReviewReportRes createReviewReport(Long userId, Long reviewId, @Valid CreateReviewReportReq req) {
        // 신고한 사용자(비회원 가능)
        User user = userId == null ? null : findUser(userId);

        // 신고된 게시물
        Review review = findReview(reviewId);

        // TODO: 정책 설정 후 다시 - 신고되자마자 처리하는 경우
        // review.setReported(true);

        // 신고 저장
        Report report = new Report(TargetType.REVIEW, reviewId, req.getReasonCategory(), req.getContent(), PENDING, user);
        reportRepository.save(report);

        return new CreateReviewReportRes();
    }

    /**
     * 사용자 찾기
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    /**
     * 리뷰 찾기
     */
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new GlobalException(NOT_FOUND_REVIEW));
    }
}
