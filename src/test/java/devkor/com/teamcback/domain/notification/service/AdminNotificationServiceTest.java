package devkor.com.teamcback.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.dto.request.AdminPushDispatchReq;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchDetailRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchPreviewRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushInstallationRes;
import devkor.com.teamcback.domain.notification.dto.response.PushDispatchEnqueueRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.domain.notification.resolver.PushTargetResolver;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchRepository pushDispatchRepository;

    @Mock
    private PushMessageRepository pushMessageRepository;

    @Mock
    private PushTargetResolver pushTargetResolver;

    @Mock
    private PushPayloadFactory pushPayloadFactory;

    @Mock
    private PushDispatchService pushDispatchService;

    @Test
    void searchInstallationsDoesNotExposeExpoPushToken() throws Exception {
        PushInstallation installation = installation(AppVariant.DEV);
        when(pushInstallationRepository.findAllByUserIdOrderByModifiedAtDescPushInstallationIdDesc(1L))
                .thenReturn(List.of(installation));

        List<AdminPushInstallationRes> response = service(false)
                .searchInstallations(1L, null, null);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json).contains("installationId");
        assertThat(json).doesNotContain("expoPushToken");
        assertThat(json).doesNotContain("ExponentPushToken");
    }

    @Test
    void searchInstallationsFiltersByAppVariantWithinTheSelectedBackend() {
        PushInstallation devInstallation = installation(AppVariant.DEV);
        PushInstallation productionInstallation = installation(AppVariant.PRODUCTION);
        ReflectionTestUtils.setField(productionInstallation, "pushInstallationId", 101L);
        when(pushInstallationRepository.findAllByUserIdOrderByModifiedAtDescPushInstallationIdDesc(1L))
                .thenReturn(List.of(devInstallation, productionInstallation));

        List<AdminPushInstallationRes> response = service(false)
                .searchInstallations(1L, null, AppVariant.PRODUCTION);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).appVariant()).isEqualTo(AppVariant.PRODUCTION);
    }

    @Test
    void previewResolvesTargetAndCreatesPayloadWithoutSavingDispatchOrMessages() {
        PushInstallation installation = installation(AppVariant.DEV);
        PushPayload payload = payload();

        when(pushTargetResolver.resolveForPreview(PushTargetType.INSTALLATION, "install-1", AppVariant.DEV))
                .thenReturn(List.of(installation));
        when(pushPayloadFactory.createForPreDispatchValidation(
                "title",
                "body",
                PushMode.TEST,
                AppVariant.DEV,
                PushActionType.TEST,
                Map.of(),
                null
        )).thenReturn(payload);

        AdminPushDispatchPreviewRes response = service(false).preview(testRequest());

        assertThat(response.recipientCount()).isEqualTo(1);
        assertThat(response.recipients()).hasSize(1);
        assertThat(response.recipients().get(0).installationId()).isEqualTo("install-1");
        assertThat(response.payload()).isSameAs(payload);
        verify(pushDispatchRepository, never()).save(any(PushDispatch.class));
        verify(pushMessageRepository, never()).saveAll(any());
        verify(pushDispatchService, never()).enqueue(any(PushDispatchCommand.class));
    }

    @Test
    void testModeRejectsUserTarget() {
        AdminPushDispatchReq request = new AdminPushDispatchReq(
                PushMode.TEST,
                AppVariant.DEV,
                PushTargetType.USER,
                "1",
                "title",
                "body",
                PushActionType.TEST,
                Map.of(),
                null
        );

        assertThatThrownBy(() -> service(false).preview(request))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);
    }

    @Test
    void productionActualIsRejectedWhenProductionEnabledIsFalse() {
        assertThatThrownBy(() -> service(false).enqueue(
                1L,
                "key-1",
                productionRequest(true)
        ))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.FORBIDDEN);

        verify(pushDispatchService, never()).enqueue(any(PushDispatchCommand.class));
    }

    @Test
    void productionActualRequiresConfirmTrue() {
        assertThatThrownBy(() -> service(true).enqueue(
                1L,
                "key-1",
                productionRequest(false)
        ))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);

        verify(pushDispatchService, never()).enqueue(any(PushDispatchCommand.class));
    }

    @Test
    void enqueueDelegatesToPushDispatchService() {
        PushDispatch dispatch = dispatch(PushMode.TEST, AppVariant.DEV, PushTargetType.INSTALLATION);
        PushDispatchEnqueueRes enqueueResponse = new PushDispatchEnqueueRes(dispatch);
        when(pushDispatchService.enqueue(any(PushDispatchCommand.class)))
                .thenReturn(enqueueResponse);

        PushDispatchEnqueueRes response = service(false).enqueue(
                7L,
                "key-1",
                testRequest()
        );

        assertThat(response).isSameAs(enqueueResponse);

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        PushDispatchCommand command = captor.getValue();
        assertThat(command.notificationType()).isEqualTo(NotificationType.GENERAL);
        assertThat(command.mode()).isEqualTo(PushMode.TEST);
        assertThat(command.appVariant()).isEqualTo(AppVariant.DEV);
        assertThat(command.targetType()).isEqualTo(PushTargetType.INSTALLATION);
        assertThat(command.idempotencyKey()).isEqualTo("key-1");
        assertThat(command.createdBy()).isEqualTo(7L);
    }

    @Test
    void actualAllTargetsOnlyActiveInstallationsResolvedForTheVariant() {
        PushDispatch dispatch = dispatch(PushMode.ACTUAL, AppVariant.DEV, PushTargetType.ALL);
        PushDispatchEnqueueRes enqueueResponse = new PushDispatchEnqueueRes(dispatch);
        when(pushTargetResolver.resolveForPreview(PushTargetType.ALL, "ALL", AppVariant.DEV))
                .thenReturn(List.of(installation(AppVariant.DEV)));
        when(pushPayloadFactory.createForPreDispatchValidation(
                "title",
                "body",
                PushMode.ACTUAL,
                AppVariant.DEV,
                PushActionType.HOME,
                Map.of(),
                null
        )).thenReturn(payload());
        when(pushDispatchService.enqueue(any(PushDispatchCommand.class)))
                .thenReturn(enqueueResponse);

        AdminPushDispatchReq request = new AdminPushDispatchReq(
                PushMode.ACTUAL,
                AppVariant.DEV,
                PushTargetType.ALL,
                "ALL",
                "title",
                "body",
                PushActionType.HOME,
                Map.of(),
                false
        );

        AdminPushDispatchPreviewRes preview = service(false).preview(request);
        PushDispatchEnqueueRes response = service(false).enqueue(7L, "broadcast-dev-1", request);

        assertThat(preview.recipientCount()).isEqualTo(1);
        assertThat(response).isSameAs(enqueueResponse);
        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().targetType()).isEqualTo(PushTargetType.ALL);
        assertThat(captor.getValue().targetValue()).isEqualTo("ALL");
        assertThat(captor.getValue().appVariant()).isEqualTo(AppVariant.DEV);
    }

    @Test
    void selectedTargetValuesAreForwardedToTheDispatchCommand() {
        PushDispatch dispatch = dispatch(PushMode.ACTUAL, AppVariant.DEV, PushTargetType.INSTALLATION);
        PushDispatchEnqueueRes enqueueResponse = new PushDispatchEnqueueRes(dispatch);
        when(pushDispatchService.enqueue(any(PushDispatchCommand.class)))
                .thenReturn(enqueueResponse);

        AdminPushDispatchReq request = new AdminPushDispatchReq(
                PushMode.ACTUAL,
                AppVariant.DEV,
                PushTargetType.INSTALLATION,
                "SELECTED(2)",
                List.of("install-1", "install-2"),
                "title",
                "body",
                PushActionType.HOME,
                Map.of(),
                false
        );

        service(false).enqueue(7L, "selected-1", request);

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().targetValues()).containsExactly("install-1", "install-2");
    }

    @Test
    void imageUrlIsForwardedToTheDispatchCommand() {
        PushDispatch dispatch = dispatch(PushMode.ACTUAL, AppVariant.DEV, PushTargetType.INSTALLATION);
        PushDispatchEnqueueRes enqueueResponse = new PushDispatchEnqueueRes(dispatch);
        when(pushDispatchService.enqueue(any(PushDispatchCommand.class)))
                .thenReturn(enqueueResponse);

        AdminPushDispatchReq request = new AdminPushDispatchReq(
                PushMode.ACTUAL,
                AppVariant.DEV,
                PushTargetType.INSTALLATION,
                "install-1",
                null,
                "title",
                "body",
                "https://cdn.kodaero.store/push/building.png",
                PushActionType.HOME,
                Map.of(),
                false
        );

        service(false).enqueue(7L, "image-1", request);

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().imageUrl()).isEqualTo("https://cdn.kodaero.store/push/building.png");
    }

    @Test
    void selectedTargetValuesRequireInstallationTargetType() {
        AdminPushDispatchReq request = new AdminPushDispatchReq(
                PushMode.ACTUAL,
                AppVariant.DEV,
                PushTargetType.ALL,
                "ALL",
                List.of("install-1"),
                "title",
                "body",
                PushActionType.HOME,
                Map.of(),
                false
        );

        assertThatThrownBy(() -> service(false).enqueue(7L, "selected-2", request))
                .isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_INPUT);

        verify(pushDispatchService, never()).enqueue(any(PushDispatchCommand.class));
    }

    @Test
    void getDispatchCountsAllMessageStatuses() {
        PushDispatch dispatch = dispatch(PushMode.ACTUAL, AppVariant.DEV, PushTargetType.USER);
        ReflectionTestUtils.setField(dispatch, "pushDispatchId", 10L);
        dispatch.updateRecipientCount(5);

        when(pushDispatchRepository.findById(10L)).thenReturn(Optional.of(dispatch));
        when(pushMessageRepository.countStatusesByDispatchIds(List.of(10L)))
                .thenReturn(List.of(
                        new StatusCount(10L, PushMessageStatus.QUEUED, 1L),
                        new StatusCount(10L, PushMessageStatus.SENDING, 1L),
                        new StatusCount(10L, PushMessageStatus.DELIVERED, 2L),
                        new StatusCount(10L, PushMessageStatus.FAILED, 1L)
                ));

        AdminPushDispatchDetailRes response = service(false).getDispatch(10L);

        assertThat(response.recipientCount()).isEqualTo(5);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.QUEUED, 1L);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.SENDING, 1L);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.TICKET_RECEIVED, 0L);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.RECEIPT_PENDING, 0L);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.DELIVERED, 2L);
        assertThat(response.messageStatusCounts()).containsEntry(PushMessageStatus.FAILED, 1L);
    }

    private AdminNotificationService service(boolean productionEnabled) {
        AdminNotificationService service = new AdminNotificationService(
                pushInstallationRepository,
                pushDispatchRepository,
                pushMessageRepository,
                pushTargetResolver,
                pushPayloadFactory,
                pushDispatchService
        );
        ReflectionTestUtils.setField(service, "productionEnabled", productionEnabled);
        return service;
    }

    private AdminPushDispatchReq testRequest() {
        return new AdminPushDispatchReq(
                PushMode.TEST,
                AppVariant.DEV,
                PushTargetType.INSTALLATION,
                "install-1",
                "title",
                "body",
                PushActionType.TEST,
                Map.of(),
                null
        );
    }

    private AdminPushDispatchReq productionRequest(boolean confirm) {
        return new AdminPushDispatchReq(
                PushMode.ACTUAL,
                AppVariant.PRODUCTION,
                PushTargetType.INSTALLATION,
                "install-1",
                "title",
                "body",
                PushActionType.HOME,
                Map.of(),
                confirm
        );
    }

    private PushInstallation installation(AppVariant appVariant) {
        PushInstallation installation = new PushInstallation(
                1L,
                "install-1",
                "ExponentPushToken[token]",
                appVariant
        );
        ReflectionTestUtils.setField(installation, "pushInstallationId", 100L);
        ReflectionTestUtils.setField(installation, "createdAt", LocalDateTime.parse("2026-08-04T10:00:00"));
        ReflectionTestUtils.setField(installation, "modifiedAt", LocalDateTime.parse("2026-08-04T11:00:00"));
        return installation;
    }

    private PushPayload payload() {
        return new PushPayload(
                "title",
                "body",
                new PushPayload.PushPayloadData(
                        1,
                        "00000000-0000-4000-8000-000000000000",
                        "dev",
                        new PushPayload.PushPayloadAction(PushActionType.TEST.name(), Map.of())
                )
        );
    }

    private PushDispatch dispatch(
            PushMode mode,
            AppVariant appVariant,
            PushTargetType targetType
    ) {
        PushDispatch dispatch = new PushDispatch(
                NotificationType.GENERAL,
                mode,
                appVariant,
                targetType,
                targetType == PushTargetType.USER ? "1" : "install-1",
                "title",
                "body",
                mode == PushMode.TEST ? PushActionType.TEST : PushActionType.HOME,
                "{}",
                "key-1",
                1L,
                LocalDateTime.parse("2026-08-04T12:00:00")
        );
        ReflectionTestUtils.setField(dispatch, "pushDispatchId", 10L);
        return dispatch;
    }

    private static class StatusCount implements PushMessageRepository.PushDispatchMessageStatusCount {

        private final Long dispatchId;
        private final PushMessageStatus status;
        private final long count;

        private StatusCount(
                Long dispatchId,
                PushMessageStatus status,
                long count
        ) {
            this.dispatchId = dispatchId;
            this.status = status;
            this.count = count;
        }

        @Override
        public Long getDispatchId() {
            return dispatchId;
        }

        @Override
        public PushMessageStatus getStatus() {
            return status;
        }

        @Override
        public long getCount() {
            return count;
        }
    }
}
