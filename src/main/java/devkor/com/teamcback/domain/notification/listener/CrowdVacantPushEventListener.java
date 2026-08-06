package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.notification.template.DomainPushContentFactory;
import devkor.com.teamcback.domain.notification.template.PushContent;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrowdVacantPushEventListener {

    private static final Long SYSTEM_CREATED_BY = 0L;

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchService pushDispatchService;
    private final PushEventFlagService pushEventFlagService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PlaceBecameVacantEvent event) {
        if (!pushEventFlagService.isEnabled(PushEventType.CROWD)) {
            return;
        }

        try {
            Place place = placeRepository.findById(event.placeId())
                    .orElse(null);
            if (place == null) {
                log.warn("crowd vacant push skipped: place not found, placeId={}", event.placeId());
                return;
            }

            Set<Long> userIds = new LinkedHashSet<>(
                    categoryRepository.findDistinctUserIdsByLocationTypeAndLocationId(
                            LocationType.PLACE,
                            event.placeId()
                    )
            );
            if (userIds.isEmpty()) {
                return;
            }

            PushContent content = DomainPushContentFactory.placeBecameVacant(
                    place.getBuilding() == null ? null : place.getBuilding().getName(),
                    place.getName()
            );
            for (Long userId : userIds) {
                enqueueIfPushTargetExists(event, userId, content);
            }
        } catch (Exception e) {
            log.warn(
                    "crowd vacant push failed: placeId={}, bleDataId={}, error={}",
                    event.placeId(),
                    event.bleDataId(),
                    e.getMessage()
            );
        }
    }

    private void enqueueIfPushTargetExists(
            PlaceBecameVacantEvent event,
            Long userId,
            PushContent content
    ) {
        if (userId == null) {
            return;
        }

        for (AppVariant targetAppVariant : targetAppVariants()) {
            if (!pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(userId, targetAppVariant)) {
                continue;
            }

            pushDispatchService.enqueue(new PushDispatchCommand(
                    NotificationType.GENERAL,
                    PushMode.ACTUAL,
                    targetAppVariant,
                    PushTargetType.USER,
                    String.valueOf(userId),
                    content.title(),
                    content.body(),
                    PushActionType.PLACE_DETAIL,
                    Map.of("placeId", event.placeId()),
                    "crowd-vacant:%d:%d:%d:%s".formatted(
                            event.placeId(),
                            userId,
                            event.bleDataId(),
                            targetAppVariant.name().toLowerCase()
                    ),
                    SYSTEM_CREATED_BY
            ));
        }
    }

    private List<AppVariant> targetAppVariants() {
        List<AppVariant> configuredVariants = pushEventFlagService.getTargetAppVariants();
        return configuredVariants == null || configuredVariants.isEmpty()
                ? List.of(AppVariant.PRODUCTION)
                : configuredVariants;
    }
}
