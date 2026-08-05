package devkor.com.teamcback.domain.character.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.character.dto.request.CreateCharacterReq;
import devkor.com.teamcback.domain.character.dto.response.CreateCharacterRes;
import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.UserCharacter;
import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import devkor.com.teamcback.infra.s3.FilePath;
import devkor.com.teamcback.infra.s3.S3Util;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminStoreServiceTest {
    @InjectMocks
    AdminStoreService adminStoreService;

    @Mock
    CharacterRepository characterRepository;
    @Mock
    UserCharacterRepository userCharacterRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    S3Util s3Util;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @DisplayName("캐릭터 생성 시 S3 업로드 후 URL과 가격 저장")
    @Test
    void createCharacter() {
        MockMultipartFile image = new MockMultipartFile("image", "tiger.png", "image/png", new byte[] {1});
        CreateCharacterReq req = new CreateCharacterReq();
        req.setName("아기 호랑이");
        req.setPrice(10);
        req.setDisplayOrder(1);
        req.setImage(image);

        when(s3Util.uploadFile(image, FilePath.CHARACTER)).thenReturn("https://s3/character/tiger.png");
        when(characterRepository.save(any(KoCharacter.class))).thenAnswer(invocation -> {
            KoCharacter character = invocation.getArgument(0);
            ReflectionTestUtils.setField(character, "characterId", 1L);
            return character;
        });

        CreateCharacterRes res = adminStoreService.createCharacter(req);

        assertEquals(1L, res.getCharacterId());
        verify(s3Util).uploadFile(image, FilePath.CHARACTER);
    }

    @DisplayName("이미지 없이 캐릭터 생성 시 예외")
    @Test
    void createCharacterWithoutImage() {
        CreateCharacterReq req = new CreateCharacterReq();
        req.setName("아기 호랑이");
        req.setPrice(10);

        GlobalException e = assertThrows(GlobalException.class,
            () -> adminStoreService.createCharacter(req));
        assertEquals(ResultCode.INVALID_INPUT, e.getResultCode());
        verify(characterRepository, never()).save(any());
    }

    @DisplayName("가격이 없거나 음수면 생성 불가")
    @Test
    void createCharacterInvalidPrice() {
        CreateCharacterReq req = new CreateCharacterReq();
        req.setName("아기 호랑이");
        req.setPrice(-1);

        GlobalException e = assertThrows(GlobalException.class,
            () -> adminStoreService.createCharacter(req));
        assertEquals(ResultCode.INVALID_INPUT, e.getResultCode());

        req.setPrice(null);
        assertThrows(GlobalException.class, () -> adminStoreService.createCharacter(req));
    }

    @DisplayName("해금 레벨이 1~5 범위를 벗어나면 생성 불가")
    @Test
    void createCharacterInvalidRequiredLevel() {
        CreateCharacterReq req = new CreateCharacterReq();
        req.setName("아기 호랑이");
        req.setPrice(10);
        req.setRequiredLevel(0);

        GlobalException e = assertThrows(GlobalException.class,
            () -> adminStoreService.createCharacter(req));
        assertEquals(ResultCode.INVALID_INPUT, e.getResultCode());

        req.setRequiredLevel(6); // Level enum 최대(5) 초과
        assertThrows(GlobalException.class, () -> adminStoreService.createCharacter(req));
        verify(characterRepository, never()).save(any());
    }

    @DisplayName("캐릭터 수정: 이미지 미첨부 시 기존 이미지 유지, 첨부 시 교체 후 기존 파일 삭제")
    @Test
    void modifyCharacter() {
        KoCharacter character = new KoCharacter("애기호랑이", null, "옛 대사", "old-url", 0, 1, 1, true);
        when(characterRepository.findById(1L)).thenReturn(Optional.of(character));

        devkor.com.teamcback.domain.character.dto.request.ModifyCharacterReq req =
            new devkor.com.teamcback.domain.character.dto.request.ModifyCharacterReq();
        req.setName("애기호랑이");
        req.setQuote("나 호랑이 맞아요?");
        req.setPrice(0);
        req.setRequiredLevel(1);
        req.setDisplayOrder(1);

        // 이미지 미첨부 → 기존 URL 유지, S3 접근 없음
        adminStoreService.modifyCharacter(1L, req);
        assertEquals("old-url", character.getImageUrl());
        assertEquals("나 호랑이 맞아요?", character.getQuote());
        verify(s3Util, never()).deleteFile(any(String.class), any(FilePath.class));

        // 이미지 첨부 → 새 URL로 교체 + 기존 S3 파일 삭제
        MockMultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[] {1});
        req.setImage(image);
        when(s3Util.uploadFile(image, FilePath.CHARACTER)).thenReturn("new-url");

        adminStoreService.modifyCharacter(1L, req);
        assertEquals("new-url", character.getImageUrl());
        verify(s3Util).deleteFile(eq("old-url"), eq(FilePath.CHARACTER));
    }

    @DisplayName("구매한 사용자가 있는 캐릭터 삭제 시 예외")
    @Test
    void deleteOwnedCharacterRejected() {
        KoCharacter character = new KoCharacter("아기 호랑이", null, null, "url", 10, 1, 1, true);
        when(characterRepository.findById(1L)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByCharacter(character)).thenReturn(true);

        GlobalException e = assertThrows(GlobalException.class,
            () -> adminStoreService.deleteCharacter(1L));
        assertEquals(ResultCode.CHARACTER_IN_USE, e.getResultCode());
        verify(characterRepository, never()).delete(any());
    }

    @DisplayName("구매자가 없으면 삭제 성공 (S3 이미지도 삭제)")
    @Test
    void deleteCharacter() {
        KoCharacter character = new KoCharacter("아기 호랑이", null, null, "url", 10, 1, 1, true);
        when(characterRepository.findById(1L)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByCharacter(character)).thenReturn(false);

        adminStoreService.deleteCharacter(1L);

        verify(s3Util).deleteFile(eq("url"), eq(FilePath.CHARACTER));
        verify(characterRepository).delete(character);
    }

    @DisplayName("수동 지급: 이미 보유 시 예외, 미보유 시 무료 지급")
    @Test
    void grantCharacter() {
        KoCharacter character = new KoCharacter("이벤트 캐릭터", null, null, "url", 100, 1, 1, true);
        ReflectionTestUtils.setField(character, "characterId", 1L);
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", 2L);
        when(characterRepository.findById(1L)).thenReturn(Optional.of(character));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(true);

        GlobalException e = assertThrows(GlobalException.class,
            () -> adminStoreService.grantCharacter(1L, 2L));
        assertEquals(ResultCode.ALREADY_OWNED_CHARACTER, e.getResultCode());

        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(false);
        when(userCharacterRepository.saveAndFlush(any(UserCharacter.class))).thenAnswer(invocation -> {
            UserCharacter userCharacter = invocation.getArgument(0);
            ReflectionTestUtils.setField(userCharacter, "userCharacterId", 5L);
            return userCharacter;
        });

        assertEquals(5L, adminStoreService.grantCharacter(1L, 2L).getUserCharacterId());
        assertEquals(0L, user.getPoint()); // 지급은 포인트를 건드리지 않음

        ArgumentCaptor<CharacterUnlockedEvent> eventCaptor = ArgumentCaptor.forClass(CharacterUnlockedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(2L, eventCaptor.getValue().userId());
        assertEquals(5L, eventCaptor.getValue().userCharacterId());
    }
}
