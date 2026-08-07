package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterUnlockedPushEventListenerTest {

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchService pushDispatchService;

    @Mock
    private PushEventFlagService pushEventFlagService;

    private CharacterUnlockedPushEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CharacterUnlockedPushEventListener(
                pushInstallationRepository,
                pushDispatchService,
                pushEventFlagService
        );
    }

    @Test
    void createsCharacterStoreDispatch() {
        when(pushEventFlagService.isEnabled(PushEventType.CHARACTER)).thenReturn(true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, "아기 호랑이"));

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        PushDispatchCommand command = captor.getValue();
        assertThat(command.targetType()).isEqualTo(PushTargetType.USER);
        assertThat(command.actionType()).isEqualTo(PushActionType.CHARACTER_STORE);
        assertThat(command.actionParams()).isEmpty();
        assertThat(command.title()).isEqualTo("새 캐릭터가 기다리고 있어요!");
        assertThat(command.body()).isEqualTo("아기 호랑이을 만나러 가볼까요?");
        assertThat(command.idempotencyKey()).isEqualTo("character-unlock:7:4:44:production");
    }

    @Test
    void createsSeparateDevAndProductionDispatches() {
        when(pushEventFlagService.isEnabled(PushEventType.CHARACTER)).thenReturn(true);
        when(pushEventFlagService.getTargetAppVariants()).thenReturn(List.of(AppVariant.DEV, AppVariant.PRODUCTION));
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.DEV))
                .thenReturn(true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, "아기 호랑이"));

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService, org.mockito.Mockito.times(2)).enqueue(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PushDispatchCommand::appVariant)
                .containsExactly(AppVariant.DEV, AppVariant.PRODUCTION);
        assertThat(captor.getAllValues())
                .extracting(PushDispatchCommand::idempotencyKey)
                .containsExactly(
                        "character-unlock:7:4:44:dev",
                        "character-unlock:7:4:44:production"
                );
    }

    @Test
    void usesSafeBodyWhenCharacterNameIsBlank() {
        when(pushEventFlagService.isEnabled(PushEventType.CHARACTER)).thenReturn(true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, " "));

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().body()).isEqualTo("새로운 캐릭터을 만나러 가볼까요?");
    }

    @Test
    void doesNotCreateDispatchWhenFeatureFlagIsFalse() {
        when(pushEventFlagService.isEnabled(PushEventType.CHARACTER)).thenReturn(false);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, "아기 호랑이"));

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotCreateDispatchWhenUserHasNoProductionInstallation() {
        when(pushEventFlagService.isEnabled(PushEventType.CHARACTER)).thenReturn(true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(false);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, "아기 호랑이"));

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }
}
