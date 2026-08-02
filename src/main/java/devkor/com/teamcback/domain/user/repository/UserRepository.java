package devkor.com.teamcback.domain.user.repository;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameAndUserIdNot(String username, Long id);

    boolean existsByUsername(String username);

    User findByEmailAndProvider(String email, Provider provider);

    User findByUserId(long userId);

    /**
     * score 기준으로 level 컬럼 백필 (레벨 구간: 0/5/20/40/60, Level enum과 동기화 유지)
     * level이 null이거나 score와 불일치하는 행만 갱신하므로 멱등
     */
    @Transactional
    @Modifying
    @Query(value = """
        UPDATE tb_user SET level =
          CASE WHEN score >= 60 THEN 'LEVEL5' WHEN score >= 40 THEN 'LEVEL4'
               WHEN score >= 20 THEN 'LEVEL3' WHEN score >= 5 THEN 'LEVEL2' ELSE 'LEVEL1' END
        WHERE level IS NULL OR level <>
          CASE WHEN score >= 60 THEN 'LEVEL5' WHEN score >= 40 THEN 'LEVEL4'
               WHEN score >= 20 THEN 'LEVEL3' WHEN score >= 5 THEN 'LEVEL2' ELSE 'LEVEL1' END
        """, nativeQuery = true)
    int backfillLevels();

    /**
     * point 컬럼 백필: 기존 사용자는 지금까지 적립한 score만큼 포인트를 보유한 것으로 초기화.
     * NULL(백필 전 표식)인 행만 갱신하므로 재실행해도 이미 사용한 포인트가 복구되지 않는다 (멱등)
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE tb_user SET point = score WHERE point IS NULL", nativeQuery = true)
    int backfillPoints();

    /**
     * 포인트 차감. 잔액 검증과 차감을 단일 UPDATE로 수행하여 동시 구매 시 이중 차감을 방지한다.
     * @return 1이면 차감 성공, 0이면 잔액 부족
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.point = u.point - :price where u.userId = :userId and u.point >= :price")
    int deductPoint(@Param("userId") Long userId, @Param("price") int price);
}
