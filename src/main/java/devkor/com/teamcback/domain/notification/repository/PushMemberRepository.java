package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.user.entity.User;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Member identity and authorization always stay on the primary service database. */
public interface PushMemberRepository extends Repository<User, Long> {
    @Query("select u.userId as userId, u.username as username, u.email as email from User u where u.userId in :ids")
    List<PushInstallationRepository.UserProfile> profiles(@Param("ids") Collection<Long> ids);
    @Query("select u.userId from User u where u.userId in :ids and u.role = devkor.com.teamcback.domain.user.entity.Role.ADMIN")
    Set<Long> admins(@Param("ids") Collection<Long> ids);
    @Query("""
        select u.userId from User u where u.userId > :after
        and (:adminOnly = false or u.role = devkor.com.teamcback.domain.user.entity.Role.ADMIN)
        and (lower(u.username) like lower(:pattern) escape '!' or lower(u.email) like lower(:pattern) escape '!')
        order by u.userId
        """)
    List<Long> search(@Param("pattern") String pattern, @Param("adminOnly") boolean adminOnly,
            @Param("after") Long after, Pageable page);
}
