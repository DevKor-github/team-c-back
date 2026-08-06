package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.NotificationType;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import devkor.com.teamcback.domain.notification.template.DomainPushContentFactory;
import devkor.com.teamcback.domain.notification.template.PushContent;
import java.util.Map;
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
public class CharacterUnlockedPushEventListener {

    private static final Long SYSTEM_CREATED_BY = 0L;

    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchService pushDispatchService;
    private final PushEventFlagService pushEventFlagService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CharacterUnlockedEvent event) {
        if (!pushEventFlagService.isEnabled(PushEventType.CHARACTER)) {
            return;
        }

        try {
            if (!pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(
                    event.userId(),
                    AppVariant.PRODUCTION
            )) {
                return;
            }

            PushContent content = DomainPushContentFactory.characterUnlocked(event.characterName());
            pushDispatchService.enqueue(new PushDispatchCommand(
                    NotificationType.GENERAL,
                    PushMode.ACTUAL,
                    AppVariant.PRODUCTION,
                    PushTargetType.USER,
                    String.valueOf(event.userId()),
                    content.title(),
                    content.body(),
                    PushActionType.CHARACTER_STORE,
                    Map.of(),
                    "character-unlock:%d:%d:%d".formatted(
                            event.userId(),
                            event.characterId(),
                            event.userCharacterId()
                    ),
                    SYSTEM_CREATED_BY
            ));
        } catch (Exception e) {
            log.warn(
                    "character unlock push failed: userId={}, characterId={}, userCharacterId={}, error={}",
                    event.userId(),
                    event.characterId(),
                    event.userCharacterId(),
                    e.getMessage()
            );
        }
    }
}
