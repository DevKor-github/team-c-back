package devkor.com.teamcback.domain.character.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.character.dto.response.GetStoreCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetStoreRes;
import devkor.com.teamcback.domain.character.dto.response.PurchaseCharacterRes;
import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.PurchaseStatus;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {
    @InjectMocks
    StoreService storeService;

    @Mock
    CharacterRepository characterRepository;
    @Mock
    UserCharacterRepository userCharacterRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    static final Long USER_ID = 1L;
    static final Long CHARACTER_ID = 10L;

    User user;
    KoCharacter character; // 해금 레벨 1, 가격 10

    @BeforeEach
    void setUp() {
        user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", USER_ID);

        character = newCharacter(CHARACTER_ID, "아기 호랑이", 10, 1, 1, true);
    }

    private KoCharacter newCharacter(Long id, String name, int price, int requiredLevel, int order, boolean active) {
        KoCharacter c = new KoCharacter(name, null, name + " 대사", "url", price, requiredLevel, order, active);
        ReflectionTestUtils.setField(c, "characterId", id);
        return c;
    }

    @DisplayName("레벨 충족 + 포인트 충분이면 구매 성공")
    @Test
    void purchaseCharacter() {
        user.addPoint(25);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(false);
        when(userRepository.deductPoint(USER_ID, 10)).thenReturn(1);
        when(userCharacterRepository.saveAndFlush(any(UserCharacter.class)))
            .thenAnswer(invocation -> {
                UserCharacter userCharacter = invocation.getArgument(0);
                ReflectionTestUtils.setField(userCharacter, "userCharacterId", 55L);
                return userCharacter;
            });

        PurchaseCharacterRes res = storeService.purchaseCharacter(USER_ID, CHARACTER_ID);

        assertEquals(CHARACTER_ID, res.getCharacterId());
        assertEquals(10, res.getPrice());
        verify(userRepository).deductPoint(USER_ID, 10);

        ArgumentCaptor<CharacterUnlockedEvent> eventCaptor = ArgumentCaptor.forClass(CharacterUnlockedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(USER_ID, eventCaptor.getValue().userId());
        assertEquals(CHARACTER_ID, eventCaptor.getValue().characterId());
        assertEquals(55L, eventCaptor.getValue().userCharacterId());
    }

    @DisplayName("해금 레벨 미달이면 포인트가 충분해도 구매 불가")
    @Test
    void purchaseInsufficientLevel() {
        KoCharacter level3Character = newCharacter(CHARACTER_ID, "청년 호랑이", 10, 3, 3, true);
        user.addPoint(100); // 포인트는 충분
        // 유저는 score 0 → LEVEL1
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(level3Character));
        when(userCharacterRepository.existsByUserAndCharacter(user, level3Character)).thenReturn(false);

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.INSUFFICIENT_LEVEL, e.getResultCode());
        verify(userRepository, never()).deductPoint(any(), org.mockito.ArgumentMatchers.anyInt());

        // 레벨 3 도달 시 구매 가능
        user.updateScore(20L, false); // LEVEL3
        when(userRepository.deductPoint(USER_ID, 10)).thenReturn(1);
        when(userCharacterRepository.saveAndFlush(any(UserCharacter.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(CHARACTER_ID, storeService.purchaseCharacter(USER_ID, CHARACTER_ID).getCharacterId());
    }

    @DisplayName("포인트 부족 시 구매 실패")
    @Test
    void purchaseInsufficientPoint() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(false);
        when(userRepository.deductPoint(USER_ID, 10)).thenReturn(0); // 잔액 부족

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.INSUFFICIENT_POINT, e.getResultCode());
        verify(userCharacterRepository, never()).saveAndFlush(any());
    }

    @DisplayName("이미 보유한 캐릭터 구매 시 예외 (차감 없음)")
    @Test
    void purchaseAlreadyOwned() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(true);

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.ALREADY_OWNED_CHARACTER, e.getResultCode());
        verify(userRepository, never()).deductPoint(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @DisplayName("비활성 캐릭터는 구매 불가")
    @Test
    void purchaseInactiveRejected() {
        KoCharacter inactive = newCharacter(CHARACTER_ID, "숨김 캐릭터", 10, 1, 9, false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(inactive));

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.INACTIVE_CHARACTER, e.getResultCode());
    }

    @DisplayName("동시 중복 구매 시 제약 위반을 이미 보유로 매핑 (롤백으로 차감 복구)")
    @Test
    void purchaseRaceMappedToAlreadyOwned() {
        user.addPoint(25);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(false);
        when(userRepository.deductPoint(USER_ID, 10)).thenReturn(1);
        when(userCharacterRepository.saveAndFlush(any(UserCharacter.class)))
            .thenThrow(new DataIntegrityViolationException("uk_user_character"));

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.ALREADY_OWNED_CHARACTER, e.getResultCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @DisplayName("미보유 캐릭터 장착 시 예외, 보유 캐릭터는 장착/해제 성공")
    @Test
    void equipCharacter() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(false);

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.equipCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.NOT_OWNED_CHARACTER, e.getResultCode());

        when(userCharacterRepository.existsByUserAndCharacter(user, character)).thenReturn(true);
        storeService.equipCharacter(USER_ID, CHARACTER_ID);
        assertEquals(CHARACTER_ID, user.getEquippedCharacterId());

        storeService.unequipCharacter(USER_ID);
        assertNull(user.getEquippedCharacterId());
    }

    @DisplayName("스토어 목록: 4단계 상태 판정 (보유/구매가능/미해금/포인트부족)")
    @Test
    void getStoreStatuses() {
        user.updateScore(5L, false); // LEVEL2
        user.addPoint(10);
        user.updateEquippedCharacter(CHARACTER_ID);

        KoCharacter ownedCharacter = character;                                              // 보유 + 장착
        KoCharacter purchasableCharacter = newCharacter(11L, "학생 호랑이", 10, 2, 2, true);   // 레벨·포인트 충족
        KoCharacter lockedCharacter = newCharacter(12L, "청년 호랑이", 5, 3, 3, true);         // 레벨 미달 (포인트는 충분해도 LOCKED)
        KoCharacter expensiveCharacter = newCharacter(13L, "비싼 호랑이", 999, 1, 4, true);    // 레벨 충족, 포인트 부족

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
            .thenReturn(List.of(ownedCharacter, purchasableCharacter, lockedCharacter, expensiveCharacter));
        when(userCharacterRepository.findAllByUser(user))
            .thenReturn(List.of(new UserCharacter(user, ownedCharacter)));

        GetStoreRes res = storeService.getStore(USER_ID);
        List<GetStoreCharacterRes> list = res.getCharacterList();

        assertEquals(10L, res.getPoint());
        assertEquals(PurchaseStatus.OWNED, statusOf(list, CHARACTER_ID));
        assertEquals(PurchaseStatus.PURCHASABLE, statusOf(list, 11L));
        assertEquals(PurchaseStatus.LOCKED, statusOf(list, 12L));
        assertEquals(PurchaseStatus.NOT_ENOUGH_POINT, statusOf(list, 13L));
    }

    @DisplayName("스토어 정렬: 보유(레벨→포인트) → 구매가능(포인트) → 미해금(레벨) → 포인트부족(포인트)")
    @Test
    void getStoreSorting() {
        user.updateScore(5L, false); // LEVEL2
        user.addPoint(20);

        // displayOrder는 전부 역순(9~1)으로 줘서 정렬이 displayOrder가 아닌 스펙 기준임을 증명
        KoCharacter ownedHighLevel = newCharacter(21L, "보유-레벨2", 5, 2, 9, true);
        KoCharacter ownedLowLevel = newCharacter(22L, "보유-레벨1", 10, 1, 8, true);
        KoCharacter purchasableExpensive = newCharacter(23L, "구매가능-20p", 20, 1, 7, true);
        KoCharacter purchasableCheap = newCharacter(24L, "구매가능-5p", 5, 2, 6, true);
        KoCharacter lockedLevel5 = newCharacter(25L, "미해금-레벨5", 0, 5, 5, true);
        KoCharacter lockedLevel3 = newCharacter(26L, "미해금-레벨3", 0, 3, 4, true);
        KoCharacter poorExpensive = newCharacter(27L, "포인트부족-99p", 99, 1, 3, true);
        KoCharacter poorCheap = newCharacter(28L, "포인트부족-30p", 30, 2, 2, true);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
            .thenReturn(List.of(ownedHighLevel, ownedLowLevel, purchasableExpensive, purchasableCheap,
                lockedLevel5, lockedLevel3, poorExpensive, poorCheap));
        when(userCharacterRepository.findAllByUser(user))
            .thenReturn(List.of(new UserCharacter(user, ownedHighLevel), new UserCharacter(user, ownedLowLevel)));

        List<GetStoreCharacterRes> list = storeService.getStore(USER_ID).getCharacterList();
        List<Long> orderedIds = list.stream().map(GetStoreCharacterRes::getCharacterId).toList();

        assertEquals(List.of(
            22L, 21L,  // 1순위 보유: 레벨1(가격10) → 레벨2(가격5)
            24L, 23L,  // 2순위 구매가능: 5p → 20p
            26L, 25L,  // 3순위 미해금: 레벨3 → 레벨5
            28L, 27L   // 4순위 포인트부족: 30p → 99p
        ), orderedIds);
    }

    @DisplayName("지급받아 보유 중인 캐릭터는 레벨 미달이어도 '이미 보유' 응답 (검증 순서)")
    @Test
    void purchaseOwnedTakesPrecedenceOverLevel() {
        // 운영자 지급으로 레벨 5 캐릭터를 보유한 레벨 1 사용자
        KoCharacter grantedCharacter = newCharacter(CHARACTER_ID, "어른호랑이", 30, 5, 5, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(grantedCharacter));
        when(userCharacterRepository.existsByUserAndCharacter(user, grantedCharacter)).thenReturn(true);

        GlobalException e = assertThrows(GlobalException.class,
            () -> storeService.purchaseCharacter(USER_ID, CHARACTER_ID));
        assertEquals(ResultCode.ALREADY_OWNED_CHARACTER, e.getResultCode()); // INSUFFICIENT_LEVEL이 아니어야 함
    }

    @DisplayName("가격 0 캐릭터는 포인트 0이어도 구매 가능 상태 (경계값)")
    @Test
    void freeCharacterPurchasableWithZeroPoint() {
        // 신규 사용자: score 0, point 0, LEVEL1
        KoCharacter freeCharacter = newCharacter(CHARACTER_ID, "애기호랑이", 0, 1, 1, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(characterRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(freeCharacter));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(List.of());

        GetStoreRes res = storeService.getStore(USER_ID);

        assertEquals(PurchaseStatus.PURCHASABLE, res.getCharacterList().get(0).getStatus());
    }

    @DisplayName("내 보유 캐릭터: 포인트·대표 캐릭터·장착 플래그·대사 반환")
    @Test
    void getMyCharacters() {
        user.addPoint(7);
        user.updateEquippedCharacter(CHARACTER_ID);
        KoCharacter otherCharacter = newCharacter(11L, "꼬마호랑이", 15, 2, 2, true);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(
            List.of(new UserCharacter(user, character), new UserCharacter(user, otherCharacter)));

        var res = storeService.getMyCharacters(USER_ID);

        assertEquals(7L, res.getPoint());
        assertEquals(CHARACTER_ID, res.getEquippedCharacterId());
        assertEquals(2, res.getCharacterList().size());
        assertEquals(true, res.getCharacterList().get(0).isEquipped());   // 장착한 캐릭터
        assertEquals(false, res.getCharacterList().get(1).isEquipped());  // 미장착 보유 캐릭터
        assertEquals("아기 호랑이 대사", res.getCharacterList().get(0).getQuote());
    }

    private PurchaseStatus statusOf(List<GetStoreCharacterRes> list, Long characterId) {
        return list.stream().filter(c -> c.getCharacterId().equals(characterId)).findFirst().orElseThrow().getStatus();
    }
}
