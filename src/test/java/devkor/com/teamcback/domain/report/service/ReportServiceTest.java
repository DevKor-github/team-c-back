package devkor.com.teamcback.domain.report.service;

import devkor.com.teamcback.domain.report.dto.request.UpdateReportStatusReq;
import devkor.com.teamcback.domain.report.entity.ReasonCategory;
import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import devkor.com.teamcback.domain.report.repository.ReportRepository;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportService reportService;
    private User reporter;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                reviewRepository,
                userRepository,
                eventPublisher
        );

        reporter = new User("reporter", "reporter@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(reporter, "userId", 7L);
    }

    @Test
    void publishesEventWhenPendingReportBecomesFinalStatus() {
        Report report = report(ReportStatus.PENDING, reporter);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        reportService.updateReportStatus(1L, req(ReportStatus.REJECTED));

        ArgumentCaptor<ReportResolvedEvent> captor = ArgumentCaptor.forClass(ReportResolvedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().reportId()).isEqualTo(1L);
        assertThat(captor.getValue().reporterUserId()).isEqualTo(7L);
        assertThat(captor.getValue().finalStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void doesNotPublishWhenFinalReportIsReprocessed() {
        Report report = report(ReportStatus.RESOLVED, reporter);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        reportService.updateReportStatus(1L, req(ReportStatus.REJECTED));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void doesNotPublishWhenStatusDoesNotChangeToFinal() {
        Report report = report(ReportStatus.PENDING, reporter);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        reportService.updateReportStatus(1L, req(ReportStatus.PENDING));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private UpdateReportStatusReq req(ReportStatus status) {
        UpdateReportStatusReq req = new UpdateReportStatusReq();
        req.setStatus(status);
        return req;
    }

    private Report report(
            ReportStatus status,
            User reporter
    ) {
        Report report = new Report(
                TargetType.REVIEW,
                20L,
                ReasonCategory.SPAM_OR_ADVERTISING,
                "sensitive report content",
                status,
                reporter,
                null
        );
        ReflectionTestUtils.setField(report, "id", 1L);
        return report;
    }
}
