package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportResolvedPushEventListenerTest {

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchService pushDispatchService;

    private ReportResolvedPushEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReportResolvedPushEventListener(
                pushInstallationRepository,
                pushDispatchService
        );
    }

    @Test
    void createsReporterDispatch() {
        ReflectionTestUtils.setField(listener, "reportEnabled", true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(new ReportResolvedEvent(3L, 7L, ReportStatus.REJECTED));

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        PushDispatchCommand command = captor.getValue();
        assertThat(command.targetType()).isEqualTo(PushTargetType.USER);
        assertThat(command.targetValue()).isEqualTo("7");
        assertThat(command.actionType()).isEqualTo(PushActionType.HOME);
        assertThat(command.actionParams()).isEmpty();
        assertThat(command.body()).doesNotContain("sensitive").doesNotContain("memo");
        assertThat(command.idempotencyKey()).isEqualTo("report-result:3:REJECTED:7");
    }

    @Test
    void doesNotCreateDispatchWhenFeatureFlagIsFalse() {
        ReflectionTestUtils.setField(listener, "reportEnabled", false);

        listener.handle(new ReportResolvedEvent(3L, 7L, ReportStatus.REJECTED));

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotCreateDispatchWhenReporterIsUnknown() {
        ReflectionTestUtils.setField(listener, "reportEnabled", true);

        listener.handle(new ReportResolvedEvent(3L, null, ReportStatus.REJECTED));

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }
}
