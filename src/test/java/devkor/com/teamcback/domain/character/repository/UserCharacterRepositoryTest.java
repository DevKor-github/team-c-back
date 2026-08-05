package devkor.com.teamcback.domain.character.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.UserCharacter;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
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
class UserCharacterRepositoryTest {
    @Autowired
    UserCharacterRepository userCharacterRepository;

    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    UserRepository userRepository;

    User user1;
    User user2;
    KoCharacter character;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("user1", "user1@test.com", Role.USER, Provider.KAKAO));
        user2 = userRepository.save(new User("user2", "user2@test.com", Role.USER, Provider.KAKAO));
        character = characterRepository.save(new KoCharacter("캐릭터", null, null, "url", 10, 1, 1, true));

        userCharacterRepository.save(new UserCharacter(user1, character));
    }

    @DisplayName("같은 사용자-캐릭터 중복 저장 시 제약 위반 (동시 해금 방어선)")
    @Test
    void duplicateClaimThrows() {
        assertThrows(DataIntegrityViolationException.class, () ->
            userCharacterRepository.saveAndFlush(new UserCharacter(user1, character)));
    }

    @DisplayName("사용자별 보유 캐릭터만 조회")
    @Test
    void findAllByUser() {
        List<UserCharacter> owned = userCharacterRepository.findAllByUser(user1);

        assertEquals(1, owned.size());
        assertEquals(character.getCharacterId(), owned.get(0).getCharacter().getCharacterId());
        assertTrue(userCharacterRepository.findAllByUser(user2).isEmpty());
    }

    @DisplayName("보유 여부 확인")
    @Test
    void existsByUserAndCharacter() {
        assertTrue(userCharacterRepository.existsByUserAndCharacter(user1, character));
        assertTrue(userCharacterRepository.existsByCharacter(character));
    }

    @DisplayName("회원 탈퇴 시 본인 보유 이력만 삭제")
    @Test
    void deleteAllByUser() {
        userCharacterRepository.save(new UserCharacter(user2, character));

        userCharacterRepository.deleteAllByUser(user1);

        assertTrue(userCharacterRepository.findAllByUser(user1).isEmpty());
        assertEquals(1, userCharacterRepository.findAllByUser(user2).size());
    }
}
