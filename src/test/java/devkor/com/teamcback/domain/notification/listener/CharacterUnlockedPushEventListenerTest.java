package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
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
class CharacterUnlockedPushEventListenerTest {

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchService pushDispatchService;

    private CharacterUnlockedPushEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CharacterUnlockedPushEventListener(
                pushInstallationRepository,
                pushDispatchService
        );
    }

    @Test
    void createsCharacterStoreDispatch() {
        ReflectionTestUtils.setField(listener, "characterEnabled", true);
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
        assertThat(command.idempotencyKey()).isEqualTo("character-unlock:7:4:44");
    }

    @Test
    void usesSafeBodyWhenCharacterNameIsBlank() {
        ReflectionTestUtils.setField(listener, "characterEnabled", true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(7L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, " "));

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService).enqueue(captor.capture());
        assertThat(captor.getValue().body()).isEqualTo("새로운 캐릭터을 만나러 가볼까요?");
    }

    @Test
    void doesNotCreateDispatchWhenFeatureFlagIsFalse() {
        ReflectionTestUtils.setField(listener, "characterEnabled", false);

        listener.handle(new CharacterUnlockedEvent(7L, 4L, 44L, "아기 호랑이"));

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }
}
