package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.dto.request.AdminPushDispatchReq;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchDetailRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchPreviewRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushDispatchSummaryRes;
import devkor.com.teamcback.domain.notification.dto.response.AdminPushInstallationRes;
import devkor.com.teamcback.domain.notification.dto.response.PushDispatchEnqueueRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.FORBIDDEN;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static devkor.com.teamcback.global.response.ResultCode.UNSUPPORTED_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationService {

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushTargetResolver pushTargetResolver;
    private final PushPayloadFactory pushPayloadFactory;
    private final PushDispatchService pushDispatchService;

    @Value("${push.admin.production-enabled:false}")
    private boolean productionEnabled;

    public List<AdminPushInstallationRes> searchInstallations(
            Long userId,
            String installationId,
            AppVariant appVariant
    ) {
        if ((userId == null && !hasText(installationId))
                || (userId != null && hasText(installationId))) {
            throw new GlobalException(INVALID_INPUT);
        }

        if (userId != null) {
            return pushInstallationRepository.findAllByUserIdOrderByModifiedAtDescPushInstallationIdDesc(userId)
                    .stream()
                    .filter(installation -> appVariant == null || appVariant.equals(installation.getAppVariant()))
                    .map(AdminPushInstallationRes::new)
                    .toList();
        }

        return pushInstallationRepository.findByInstallationId(installationId)
                .stream()
                .filter(installation -> appVariant == null || appVariant.equals(installation.getAppVariant()))
                .map(AdminPushInstallationRes::new)
                .toList();
    }

    public AdminPushDispatchPreviewRes preview(AdminPushDispatchReq request) {
        validateTargetRules(request);

        List<PushInstallation> installations = pushTargetResolver.resolveForPreview(
                request.targetType(),
                request.targetValue(),
                request.appVariant()
        );

        PushPayload payload = pushPayloadFactory.createForPreDispatchValidation(
                request.title(),
                request.body(),
                request.mode(),
                request.appVariant(),
                request.actionType(),
                request.actionParams()
        );

        return new AdminPushDispatchPreviewRes(
                installations.size(),
                payload
        );
    }

    @Transactional
    public PushDispatchEnqueueRes enqueue(
            Long adminUserId,
            String idempotencyKey,
            AdminPushDispatchReq request
    ) {
        validateTargetRules(request);
        validateProductionGate(request);

        return pushDispatchService.enqueue(new PushDispatchCommand(
                NotificationType.GENERAL,
                request.mode(),
                request.appVariant(),
                request.targetType(),
                request.targetValue(),
                request.title(),
                request.body(),
                request.actionType(),
                request.actionParams(),
                idempotencyKey,
                adminUserId
        ));
    }

    public Page<AdminPushDispatchSummaryRes> getDispatches(
            int page,
            int size,
            AppVariant appVariant,
            PushDispatchStatus status
    ) {
        if (page < 1 || size < 1) {
            throw new GlobalException(INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        return pushDispatchRepository.findAdminDispatches(appVariant, status, pageable)
                .map(AdminPushDispatchSummaryRes::new);
    }

    public AdminPushDispatchDetailRes getDispatch(Long dispatchId) {
        if (dispatchId == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        PushDispatch dispatch = pushDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new GlobalException(INVALID_INPUT));

        Map<PushMessageStatus, Long> statusCounts = zeroStatusCounts();
        pushMessageRepository.countStatusesByDispatchIds(List.of(dispatchId))
                .forEach(count -> statusCounts.put(count.getStatus(), count.getCount()));

        return new AdminPushDispatchDetailRes(dispatch, statusCounts);
    }

    private void validateTargetRules(AdminPushDispatchReq request) {
        if (request == null
                || request.mode() == null
                || request.appVariant() == null
                || request.targetType() == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        if (PushTargetType.USER_GROUP.equals(request.targetType())) {
            throw new GlobalException(UNSUPPORTED_REQUEST);
        }

        if (PushMode.TEST.equals(request.mode())
                && !PushTargetType.INSTALLATION.equals(request.targetType())) {
            throw new GlobalException(INVALID_INPUT);
        }

        if (PushMode.ACTUAL.equals(request.mode())
                && !PushTargetType.INSTALLATION.equals(request.targetType())
                && !PushTargetType.USER.equals(request.targetType())
                && !PushTargetType.ALL.equals(request.targetType())) {
            throw new GlobalException(UNSUPPORTED_REQUEST);
        }
    }

    private void validateProductionGate(AdminPushDispatchReq request) {
        if (!PushMode.ACTUAL.equals(request.mode())
                || !AppVariant.PRODUCTION.equals(request.appVariant())) {
            return;
        }

        if (!productionEnabled) {
            throw new GlobalException(FORBIDDEN);
        }

        if (!Boolean.TRUE.equals(request.confirm())) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private Map<PushMessageStatus, Long> zeroStatusCounts() {
        Map<PushMessageStatus, Long> statusCounts = new EnumMap<>(PushMessageStatus.class);
        for (PushMessageStatus status : PushMessageStatus.values()) {
            statusCounts.put(status, 0L);
        }
        return statusCounts;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
