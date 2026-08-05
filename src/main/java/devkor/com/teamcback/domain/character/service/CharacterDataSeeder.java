package devkor.com.teamcback.domain.character.service;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 캐릭터 확정 데이터 시더 (기획 문서 "포인트 상점 기능 > 캐릭터 DB 확정" 기준, 2026-07-29).
 * 이름 존재 여부로 행 단위 멱등 처리, 멀티 인스턴스 동시 부팅은 name UNIQUE 제약이 방어.
 * 기획 확정 전에 시드했던 구 플레이스홀더 5종은 보유자가 없을 때만 정리한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class CharacterDataSeeder implements ApplicationRunner {
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;

    // 기획 확정 전 플레이스홀더 시드 이름 (확정 DB와 이름이 달라 공존하게 되므로 제거 대상)
    private static final List<String> LEGACY_SEED_NAMES =
        List.of("아기 호랑이", "학생 호랑이", "청년 호랑이", "석사 호랑이", "호랑이 대장");

    @Value("${character.image.base-url}")
    private String imageBaseUrl;

    // 메서드 전체를 @Transactional로 묶으면 중복 키 예외를 잡아도 트랜잭션이 rollback-only로 오염되어
    // 커밋 시 UnexpectedRollbackException으로 부팅이 실패한다. save()별 개별 트랜잭션으로 둔다.
    @Override
    public void run(ApplicationArguments args) {
        removeLegacySeeds();

        List<KoCharacter> seeds = List.of(
            new KoCharacter("애기호랑이", "기본 아바타",
                "나 호랑이 맞아요?", imageUrl("01_aegi"), 0, 1, 1, true),
            new KoCharacter("꼬마호랑이", "레벨 2 달성 시 해금",
                "엄마가 발이 크면 키 크는 거래요. 저 발 엄청 커요!", imageUrl("02_kkoma"), 15, 2, 2, true),
            new KoCharacter("포동호랑이", "레벨 3 달성 시 해금",
                "어디든 가고 싶어요! 이따 어디 갈까요? 미래관? 과도?", imageUrl("03_podong"), 20, 3, 3, true),
            new KoCharacter("학생호랑이", "레벨 4 달성 시 해금",
                "안경 쓰면 똑똑해보이잖아요. 머릿속에 들어오는 건 없어요.", imageUrl("04_haksaeng"), 25, 4, 4, true),
            new KoCharacter("어른호랑이", "레벨 5 달성 시 해금",
                "뭐든 다 할 수 있을 것 같아요. 제가 못할 리 없죠!", imageUrl("05_eoreun"), 30, 5, 5, true),
            new KoCharacter("피곤 호랑이", "포인트로 바로 구매 가능",
                "대체 며칠째 밤샘인지.. 근데 오늘이 무슨 요일이죠?", imageUrl("06_pigon"), 35, 1, 6, true),
            new KoCharacter("과잠 호랑이", "포인트로 바로 구매 가능",
                "대학생, 원래 이렇게 힘든 거였어요? 살려주세요…", imageUrl("07_gwajam"), 45, 1, 7, true),
            new KoCharacter("로봇호랑이", "포인트로 바로 구매 가능",
                "다치기 싫어서 강철 슈트 입었어요. 강해보이죠?", imageUrl("08_robot"), 50, 1, 8, true),
            new KoCharacter("천사 호랑이", "포인트로 바로 구매 가능",
                "하루에 한번은 좋은 일이 생길 거예요!", imageUrl("09_cheonsa"), 80, 1, 9, true),
            new KoCharacter("악마 호랑이", "포인트로 바로 구매 가능",
                "화가 난 것 같지만 사실 부끄러움을 숨기고 있는 거에요…", imageUrl("10_akma"), 80, 1, 10, true)
        );

        int saved = 0;
        for (KoCharacter seed : seeds) {
            if(characterRepository.existsByName(seed.getName())) continue;
            try {
                characterRepository.save(seed);
                saved++;
            } catch (DataIntegrityViolationException e) { // 다른 인스턴스가 먼저 적재한 경우
                log.info("캐릭터 시드 중복 감지, 건너뜀: {}", seed.getName());
            }
        }
        if(saved > 0) {
            log.info("캐릭터 시드 데이터 {}건 적재 완료", saved);
        }
    }

    private void removeLegacySeeds() {
        for (String legacyName : LEGACY_SEED_NAMES) {
            characterRepository.findByName(legacyName).ifPresent(legacy -> {
                if(userCharacterRepository.existsByCharacter(legacy)) { // 보유자가 있으면 수동 정리 필요
                    log.warn("구 플레이스홀더 캐릭터에 보유자가 있어 삭제하지 않음: {}", legacyName);
                    return;
                }
                characterRepository.delete(legacy);
                log.info("구 플레이스홀더 캐릭터 삭제: {}", legacyName);
            });
        }
    }

    private String imageUrl(String fileName) {
        return imageBaseUrl + "/" + fileName + ".png";
    }
}
