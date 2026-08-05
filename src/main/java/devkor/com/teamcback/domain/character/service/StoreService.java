package devkor.com.teamcback.domain.character.service;

import static devkor.com.teamcback.global.response.ResultCode.ALREADY_OWNED_CHARACTER;
import static devkor.com.teamcback.global.response.ResultCode.INACTIVE_CHARACTER;
import static devkor.com.teamcback.global.response.ResultCode.INSUFFICIENT_LEVEL;
import static devkor.com.teamcback.global.response.ResultCode.INSUFFICIENT_POINT;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_CHARACTER;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;
import static devkor.com.teamcback.global.response.ResultCode.NOT_OWNED_CHARACTER;

import devkor.com.teamcback.domain.character.dto.response.EquipCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetMyCharacterListRes;
import devkor.com.teamcback.domain.character.dto.response.GetMyCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetStoreCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetStoreRes;
import devkor.com.teamcback.domain.character.dto.response.PurchaseCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.UnequipCharacterRes;
import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.PurchaseStatus;
import devkor.com.teamcback.domain.character.entity.UserCharacter;
import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final UserRepository userRepository;

    /**
     * 스토어 조회 (보유 포인트 + 캐릭터 목록)
     * 정렬: 1순위 보유(해금 레벨순→가격순) → 2순위 구매 가능(가격 낮은순)
     *      → 3순위 미해금·레벨 미달(해금 레벨순) → 4순위 포인트 부족(가격 낮은순)
     */
    @Transactional(readOnly = true)
    public GetStoreRes getStore(Long userId) {
        User user = findUser(userId);
        List<KoCharacter> characters = characterRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();

        Map<Long, UserCharacter> ownedMap = userCharacterRepository.findAllByUser(user).stream()
            .collect(Collectors.toMap(uc -> uc.getCharacter().getCharacterId(), Function.identity()));

        long point = user.getPoint();
        int userLevel = levelNumberOf(user);
        Map<Long, PurchaseStatus> statusMap = characters.stream()
            .collect(Collectors.toMap(KoCharacter::getCharacterId,
                c -> getPurchaseStatus(c, ownedMap.containsKey(c.getCharacterId()), userLevel, point)));

        List<GetStoreCharacterRes> characterList = characters.stream()
            .sorted(storeOrder(statusMap))
            .map(character -> {
                UserCharacter owned = ownedMap.get(character.getCharacterId());
                boolean isEquipped = character.getCharacterId().equals(user.getEquippedCharacterId());
                return new GetStoreCharacterRes(character, statusMap.get(character.getCharacterId()),
                    owned == null ? null : owned.getCreatedAt(), isEquipped);
            })
            .toList();

        return new GetStoreRes(user.getPoint(), characterList);
    }

    /**
     * 내 보유 캐릭터 목록 조회
     */
    @Transactional(readOnly = true)
    public GetMyCharacterListRes getMyCharacters(Long userId) {
        User user = findUser(userId);

        List<GetMyCharacterRes> characterList = userCharacterRepository.findAllByUser(user).stream()
            .map(uc -> new GetMyCharacterRes(uc,
                uc.getCharacter().getCharacterId().equals(user.getEquippedCharacterId())))
            .toList();

        return new GetMyCharacterListRes(user.getPoint(), user.getEquippedCharacterId(), characterList);
    }

    /**
     * 캐릭터 구매 (포인트 차감)
     */
    @Transactional
    public PurchaseCharacterRes purchaseCharacter(Long userId, Long characterId) {
        User user = findUser(userId);
        KoCharacter character = findCharacter(characterId);

        if(!character.isActive()) throw new GlobalException(INACTIVE_CHARACTER);

        if(userCharacterRepository.existsByUserAndCharacter(user, character)) {
            throw new GlobalException(ALREADY_OWNED_CHARACTER);
        }

        // 해금 레벨 미달이면 포인트와 무관하게 구매 불가
        if(levelNumberOf(user) < character.getRequiredLevel()) {
            throw new GlobalException(INSUFFICIENT_LEVEL);
        }

        // 잔액 검증과 차감을 단일 조건부 UPDATE로 수행 (동시 구매 시 이중 차감 방지)
        if(userRepository.deductPoint(userId, character.getPrice()) == 0) {
            throw new GlobalException(INSUFFICIENT_POINT);
        }

        // deductPoint가 영속성 컨텍스트를 비우므로 차감이 반영된 상태로 재조회
        user = findUser(userId);
        character = findCharacter(characterId);

        try {
            UserCharacter userCharacter = userCharacterRepository.saveAndFlush(new UserCharacter(user, character));
            return new PurchaseCharacterRes(userCharacter, user.getPoint());
        } catch (DataIntegrityViolationException e) { // 동시 중복 구매는 UNIQUE 제약으로 차단 (롤백으로 차감 복구)
            throw new GlobalException(ALREADY_OWNED_CHARACTER);
        }
    }

    /**
     * 대표 캐릭터 장착
     */
    @Transactional
    public EquipCharacterRes equipCharacter(Long userId, Long characterId) {
        User user = findUser(userId);
        KoCharacter character = findCharacter(characterId);

        if(!userCharacterRepository.existsByUserAndCharacter(user, character)) {
            throw new GlobalException(NOT_OWNED_CHARACTER);
        }

        user.updateEquippedCharacter(character.getCharacterId());

        return new EquipCharacterRes(character.getCharacterId());
    }

    /**
     * 대표 캐릭터 장착 해제
     */
    @Transactional
    public UnequipCharacterRes unequipCharacter(Long userId) {
        User user = findUser(userId);
        user.updateEquippedCharacter(null);

        return new UnequipCharacterRes();
    }

    private PurchaseStatus getPurchaseStatus(KoCharacter character, boolean owned, int userLevel, long point) {
        if(owned) return PurchaseStatus.OWNED;
        if(userLevel < character.getRequiredLevel()) return PurchaseStatus.LOCKED;
        return point >= character.getPrice() ? PurchaseStatus.PURCHASABLE : PurchaseStatus.NOT_ENOUGH_POINT;
    }

    private Comparator<KoCharacter> storeOrder(Map<Long, PurchaseStatus> statusMap) {
        return Comparator
            .comparingInt((KoCharacter c) -> statusTier(statusMap.get(c.getCharacterId())))
            .thenComparingInt(c -> primarySortKey(statusMap.get(c.getCharacterId()), c))
            .thenComparingInt(c -> secondarySortKey(statusMap.get(c.getCharacterId()), c))
            .thenComparing(KoCharacter::getDisplayOrder)
            .thenComparing(KoCharacter::getCharacterId);
    }

    private int statusTier(PurchaseStatus status) {
        return switch (status) {
            case OWNED -> 0;
            case PURCHASABLE -> 1;
            case LOCKED -> 2;
            case NOT_ENOUGH_POINT -> 3;
        };
    }

    private int primarySortKey(PurchaseStatus status, KoCharacter character) {
        return switch (status) {
            case OWNED, LOCKED -> character.getRequiredLevel(); // 레벨순
            case PURCHASABLE, NOT_ENOUGH_POINT -> character.getPrice(); // 포인트 낮은순
        };
    }

    private int secondarySortKey(PurchaseStatus status, KoCharacter character) {
        return status == PurchaseStatus.OWNED ? character.getPrice() : 0; // 보유는 레벨순 다음 포인트순
    }

    private int levelNumberOf(User user) {
        // 백필 전 레거시 행 방어: level 컬럼이 비어 있으면 score로 계산
        Level level = user.getLevel() != null ? user.getLevel() : Level.fromScore(user.getScore());
        return level.getLevelNumber();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private KoCharacter findCharacter(Long characterId) {
        return characterRepository.findById(characterId).orElseThrow(() -> new GlobalException(NOT_FOUND_CHARACTER));
    }
}
