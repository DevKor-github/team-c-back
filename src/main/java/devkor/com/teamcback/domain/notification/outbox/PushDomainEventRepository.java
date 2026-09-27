package devkor.com.teamcback.domain.notification.outbox;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface PushDomainEventRepository extends JpaRepository<PushDomainEvent,Long> {
    @Query(value="SELECT * FROM tb_push_domain_event WHERE audience = :audience AND completed_at IS NULL AND next_attempt_at <= :now ORDER BY event_id LIMIT 1 FOR UPDATE SKIP LOCKED",nativeQuery=true)
    List<PushDomainEvent> claim(@Param("audience") String audience,@Param("now") LocalDateTime now);
}
