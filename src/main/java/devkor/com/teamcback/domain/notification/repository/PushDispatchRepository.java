package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushDispatchRepository extends JpaRepository<PushDispatch, Long> {

    Optional<PushDispatch> findByIdempotencyKey(
            String idempotencyKey
    );

    @Query("""
            SELECT d
            FROM PushDispatch d
            WHERE (:appVariant IS NULL OR d.appVariant = :appVariant)
              AND (:status IS NULL OR d.status = :status)
            ORDER BY d.createdAt DESC, d.pushDispatchId DESC
            """)
    Page<PushDispatch> findAdminDispatches(
            @Param("appVariant") AppVariant appVariant,
            @Param("status") PushDispatchStatus status,
            Pageable pageable
    );
}
