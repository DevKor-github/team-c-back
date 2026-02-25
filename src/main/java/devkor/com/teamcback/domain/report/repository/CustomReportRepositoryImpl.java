package devkor.com.teamcback.domain.report.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.vote.dto.response.QGetVoteOptionRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static devkor.com.teamcback.domain.report.entity.QReport.report;
import static devkor.com.teamcback.domain.vote.entity.QVote.vote;
import static devkor.com.teamcback.domain.vote.entity.QVoteOption.voteOption;
import static devkor.com.teamcback.domain.vote.entity.QVoteRecord.voteRecord;

@Repository
@RequiredArgsConstructor
public class CustomReportRepositoryImpl implements CustomReportRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Report> findUniqueReportsForUserReviewReportStatus(User user, TargetType type, ReportStatus status) {
        // 서브쿼리로 각 그룹의 최신 ID 추출
        JPQLQuery<Long> subQuery = JPAExpressions
                .select(report.id.max())
                .from(report)
                .where(
                        report.reportedUser.eq(user)
                                .and(report.targetType.eq(type))
                                .and(report.status.eq(status))
                )
                .groupBy(report.targetId, report.reasonCategory)
                .where();

        return jpaQueryFactory
                .selectFrom(report)
                .where(report.id.in(subQuery))
                .orderBy(report.effectiveAt.asc())
                .fetch();
    }
}
