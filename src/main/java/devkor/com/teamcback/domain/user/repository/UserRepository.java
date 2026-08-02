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
     * 포인트 차감. 잔액 검증과 차감을 단일 UPDATE로 수행하여 동시 구매 시 이중 차감을 방지한다.
     * @return 1이면 차감 성공, 0이면 잔액 부족
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.point = u.point - :price where u.userId = :userId and u.point >= :price")
    int deductPoint(@Param("userId") Long userId, @Param("price") int price);
}
