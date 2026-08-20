package devkor.com.teamcback.domain.character.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import devkor.com.teamcback.global.config.QueryDslConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class) // @Repository인 QueryDSL 커스텀 구현체 스캔에 필요
class CharacterRepositoryTest {
    @Autowired
    CharacterRepository characterRepository;

    @BeforeEach
    void setUp() {
        characterRepository.save(new KoCharacter("두번째", null, null, "url2", 20, 2, 2, true));
        characterRepository.save(new KoCharacter("첫번째", null, null, "url1", 10, 1, 1, true));
        characterRepository.save(new KoCharacter("비활성", null, null, "url3", 30, 3, 3, false));
    }

    @DisplayName("활성 캐릭터만 정렬 순서대로 조회")
    @Test
    void findAllByIsActiveTrueOrderByDisplayOrderAsc() {
        List<KoCharacter> characters = characterRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();

        assertEquals(2, characters.size());
        assertEquals("첫번째", characters.get(0).getName());
        assertEquals("두번째", characters.get(1).getName());
    }

    @DisplayName("관리자 조회는 비활성 포함")
    @Test
    void findAllByOrderByDisplayOrderAsc() {
        assertEquals(3, characterRepository.findAllByOrderByDisplayOrderAsc().size());
    }

    @DisplayName("이름 존재 여부 확인 (시더 멱등 처리용)")
    @Test
    void existsByName() {
        assertTrue(characterRepository.existsByName("첫번째"));
        assertFalse(characterRepository.existsByName("없는이름"));
    }

    @DisplayName("이름 중복 저장 시 제약 위반 (멀티 인스턴스 시드 레이스 방어)")
    @Test
    void duplicateNameThrows() {
        assertThrows(DataIntegrityViolationException.class, () ->
            characterRepository.saveAndFlush(new KoCharacter("첫번째", null, null, "url", 10, 1, 9, true)));
    }
}
