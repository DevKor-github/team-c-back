package devkor.com.teamcback.domain.koyeon.repository;

import devkor.com.teamcback.domain.koyeon.entity.FreePubNickname;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreePubNicknameRepository extends JpaRepository<FreePubNickname, Long> {
    List<FreePubNickname> findByNicknameContaining(String nickname);
}
