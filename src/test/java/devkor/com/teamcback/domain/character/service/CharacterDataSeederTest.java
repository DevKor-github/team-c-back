package devkor.com.teamcback.domain.character.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CharacterDataSeederTest {
    @InjectMocks
    CharacterDataSeeder seeder;

    @Mock
    CharacterRepository characterRepository;
    @Mock
    UserCharacterRepository userCharacterRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seeder, "imageBaseUrl", "https://s3/character");
    }

    @DisplayName("첫 실행 시 확정 캐릭터 10건 적재")
    @Test
    void seedOnFirstRun() {
        when(characterRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(characterRepository.existsByName(anyString())).thenReturn(false);

        seeder.run(null);

        verify(characterRepository, times(10)).save(any(KoCharacter.class));
    }

    @DisplayName("이미 적재된 경우 저장하지 않음 (멱등)")
    @Test
    void skipOnSecondRun() {
        when(characterRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(characterRepository.existsByName(anyString())).thenReturn(true);

        seeder.run(null);

        verify(characterRepository, never()).save(any(KoCharacter.class));
    }

    @DisplayName("기존 캐릭터의 모호한 해금 설명을 레벨과 포인트 조건이 모두 보이도록 갱신")
    @Test
    void updateLegacyPolicyDescription() {
        KoCharacter existing = new KoCharacter(
            "꼬마호랑이", "레벨 2 달성 시 해금", "대사", "url", 15, 2, 2, true);
        when(characterRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(characterRepository.findByName("꼬마호랑이")).thenReturn(Optional.of(existing));
        when(characterRepository.existsByName(anyString())).thenReturn(false);
        when(characterRepository.existsByName("꼬마호랑이")).thenReturn(true);

        seeder.run(null);

        assertEquals("Lv.2 달성 후 15P로 구매", existing.getDescription());
        verify(characterRepository).save(existing);
    }

    @DisplayName("동시 부팅 레이스: 중복 키 예외가 나도 나머지 시드를 계속 적재")
    @Test
    void continueSeedingAfterDuplicateKeyRace() {
        when(characterRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(characterRepository.existsByName(anyString())).thenReturn(false);
        // 첫 번째 save는 다른 인스턴스가 먼저 적재해 UNIQUE 위반, 이후는 성공
        when(characterRepository.save(any(KoCharacter.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk name"))
            .thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run(null); // 예외가 전파되면 부팅 실패 → 테스트 실패

        verify(characterRepository, times(10)).save(any(KoCharacter.class));
    }

    @DisplayName("구 플레이스홀더는 보유자가 없으면 삭제, 있으면 유지")
    @Test
    void removeLegacySeeds() {
        KoCharacter orphanLegacy = new KoCharacter("아기 호랑이", null, null, "url", 0, 1, 1, true);
        KoCharacter ownedLegacy = new KoCharacter("호랑이 대장", null, null, "url", 60, 5, 5, true);
        when(characterRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(characterRepository.findByName("아기 호랑이")).thenReturn(Optional.of(orphanLegacy));
        when(characterRepository.findByName("호랑이 대장")).thenReturn(Optional.of(ownedLegacy));
        when(userCharacterRepository.existsByCharacter(orphanLegacy)).thenReturn(false);
        when(userCharacterRepository.existsByCharacter(ownedLegacy)).thenReturn(true);
        when(characterRepository.existsByName(anyString())).thenReturn(true); // 시드 자체는 스킵

        seeder.run(null);

        verify(characterRepository).delete(orphanLegacy);
        verify(characterRepository, never()).delete(ownedLegacy);
    }
}
