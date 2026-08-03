package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDispatchRepository extends JpaRepository<PushDispatch, Long> {

    Optional<PushDispatch> findByIdempotencyKey(
            String idempotencyKey
    );
}
