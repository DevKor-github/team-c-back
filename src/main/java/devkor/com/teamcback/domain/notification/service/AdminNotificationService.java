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
@Transactional(transactionManager = "pushTransactionManager", readOnly = true)
public class AdminNotificationService {

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushTargetResolver pushTargetResolver;
    private final PushPayloadFactory pushPayloadFactory;
    private final PushDispatchService pushDispatchService;

    private static final int MAX_SELECTED_TARGETS = 500;

    @Value("${push.admin.production-enabled:false}")
    private boolean productionEnabled;

    public List<AdminPushInstallationRes> searchInstallations(
            Long userId,
            String installationId,
            AppVariant appVariant
    ) {
        return searchInstallations(userId, installationId, appVariant, null, false);
    }

    public List<AdminPushInstallationRes> searchInstallations(Long userId, String installationId,
            AppVariant appVariant, String query, boolean adminOnly) {
        int criteria = (userId != null ? 1 : 0) + (hasText(installationId) ? 1 : 0) + (hasText(query) ? 1 : 0);
        if (criteria != 1) throw new GlobalException(INVALID_INPUT);
        List<PushInstallation> installations;
        if (hasText(query)) {
            String term = query.trim();
            if (term.length() < 2 || term.length() > 100) throw new GlobalException(INVALID_INPUT);
            String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            installations = pushInstallationRepository.searchByUserProfile(pattern, appVariant, adminOnly, PageRequest.of(0, 200));
        } else {
            installations = userId != null
                    ? pushInstallationRepository.findAllByUserIdOrderByModifiedAtDescPushInstallationIdDesc(userId)
                    : pushInstallationRepository.findByInstallationId(installationId).stream().toList();
            installations = installations.stream()
                    .filter(item -> appVariant == null || appVariant.equals(item.getAppVariant())).toList();
            if (adminOnly) installations = pushTargetResolver.restrictToAdmins(installations, true);
        }
        return describeInstallations(installations);
    }

    private List<AdminPushInstallationRes> describeInstallations(List<PushInstallation> installations) {
        if (installations.isEmpty()) return List.of();
        var profiles = pushInstallationRepository.findUserProfiles(
                installations.stream().map(PushInstallation::getUserId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(PushInstallationRepository.UserProfile::getUserId, profile -> profile));
        return installations.stream().map(installation -> {
            var user = profiles.get(installation.getUserId());
            return new AdminPushInstallationRes(installation,
                    user == null ? null : user.getUsername(), user == null ? null : user.getEmail());
        }).toList();
    }

    public AdminPushDispatchPreviewRes preview(AdminPushDispatchReq request) {
        validateTargetRules(request);

        List<PushInstallation> installations = hasSelectedTargets(request)
                ? pushTargetResolver.resolveSelectedForPreview(
                        request.targetValues(),
                        request.appVariant()
                )
                : pushTargetResolver.resolveForPreview(
                        request.targetType(),
                        request.targetValue(),
                        request.appVariant()
                );

        if (Boolean.TRUE.equals(request.adminOnly())) {
            installations = pushTargetResolver.restrictToAdmins(installations, true);
        }

        PushPayload payload = pushPayloadFactory.createForPreDispatchValidation(
                request.title(),
                request.body(),
                request.mode(),
                request.appVariant(),
                request.actionType(),
                request.actionParams(),
                request.imageUrl()
        );

        return new AdminPushDispatchPreviewRes(
                installations.size(),
                describeInstallations(installations),
                payload,
                Boolean.TRUE.equals(request.adminOnly())
        );
    }

    @Transactional("pushTransactionManager")
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
                request.targetValues(),
                request.title(),
                request.body(),
                request.imageUrl(),
                request.actionType(),
                request.actionParams(),
                idempotencyKey,
                adminUserId,
                request.adminOnly()
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

    public Page<AdminPushDispatchSummaryRes> getDispatches(int page, int size, AppVariant appVariant,
            PushDispatchStatus status, Boolean adminOnly, boolean legacyOnly) {
        if (adminOnly == null && !legacyOnly) return getDispatches(page, size, appVariant, status);
        if (page < 1 || size < 1 || size > 200) throw new GlobalException(INVALID_INPUT);
        return pushDispatchRepository.findScopedAdminDispatches(appVariant, status,
                Boolean.TRUE.equals(adminOnly), legacyOnly, PageRequest.of(page - 1, size))
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

        if (hasSelectedTargets(request)) {
            if (!PushTargetType.INSTALLATION.equals(request.targetType())
                    || request.targetValues().size() > MAX_SELECTED_TARGETS) {
                throw new GlobalException(INVALID_INPUT);
            }
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

    private boolean hasSelectedTargets(AdminPushDispatchReq request) {
        return request.targetValues() != null && !request.targetValues().isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
