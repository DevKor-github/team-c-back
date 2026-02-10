package devkor.com.teamcback.domain.report.service;

import devkor.com.teamcback.domain.report.dto.request.CreateReviewReportReq;
import devkor.com.teamcback.domain.report.dto.response.*;
import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.report.repository.ReportRepository;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static devkor.com.teamcback.domain.report.entity.ReportStatus.PENDING;
import static devkor.com.teamcback.domain.report.entity.ReportStatus.RESOLVED;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_REVIEW;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰에 대한 신고 작성
     */
    @Transactional
    public CreateReviewReportRes createReviewReport(Long userId, Long reviewId, @Valid CreateReviewReportReq req) {
        // 신고한 사용자(비회원 가능)
        User user = userId == null ? null : findUser(userId);

        // 신고된 게시물
        Review review = findReview(reviewId);

        // TODO: 정책 설정 후 다시 - 신고되자마자 처리하는 경우
        // review.setReported(true);

        // 신고 저장
        Report report = new Report(TargetType.REVIEW, reviewId, req.getReasonCategory(), req.getContent(), PENDING, user, review.getUser());
        reportRepository.save(report);

        return new CreateReviewReportRes();
    }

    /**
     * 사용자가 신고되었는지 확인
     */
    @Transactional(readOnly = true)
    public GetUserReviewReportStatusRes getUserReviewReportStatus(Long userId) {
        // 사용자 검색
        User user = findUser(userId);

        // TODO: 하나만 조회하는 경우 수정
        // 신고 검색
        List<Report> reportList = reportRepository.findUniqueReportsForUserReviewReportStatus(user, TargetType.REVIEW, RESOLVED);

        // 신고된 리뷰 목록 조회
        List<GetReportedReviewRes> reviewList = new ArrayList<>();
        for(Report report : reportList) {
            reviewList.add(new GetReportedReviewRes(findReview(report.getTargetId()), report.getReasonCategory().toString()));
        }

        return new GetUserReviewReportStatusRes(user, reviewList);
    }

    /**
     * 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public GetReportListRes getReportList(ReportStatus status) {
        List<Report> reportList;

        // 신고 상태에 따라 조회
        if (status == null) {
            reportList = reportRepository.findAllByOrderByCreatedAtDesc();
        }
        else {
            reportList = reportRepository.findByStatusOrderByCreatedAtDesc(status);
        }

        return new GetReportListRes(reportList.stream().map(GetReportRes::new).toList());
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
