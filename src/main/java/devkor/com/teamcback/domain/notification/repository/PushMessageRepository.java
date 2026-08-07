package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushMessageRepository extends JpaRepository<PushMessage, Long> {

    List<PushMessage> findAllByDispatch(
            PushDispatch dispatch
    );

    @Query(
            value = """
                    SELECT *
                    FROM tb_push_message
                    WHERE status = 'QUEUED'
                      AND (next_retry_at IS NULL OR next_retry_at <= :now)
                    ORDER BY created_at ASC, push_message_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<PushMessage> findDueQueuedForUpdateSkipLocked(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT *
                    FROM tb_push_message
                    WHERE status = 'RECEIPT_PENDING'
                      AND expo_ticket_id IS NOT NULL
                      AND expo_ticket_id <> ''
                      AND receipt_available_at IS NOT NULL
                      AND receipt_available_at <= :now
                    ORDER BY receipt_available_at ASC, push_message_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<PushMessage> findDueReceiptPendingForUpdateSkipLocked(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT *
                    FROM tb_push_message
                    WHERE status = 'SENDING'
                      AND updated_at <= :staleBefore
                    ORDER BY updated_at ASC, push_message_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<PushMessage> findStaleSendingForUpdateSkipLocked(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("limit") int limit
    );

    @EntityGraph(attributePaths = "dispatch")
    List<PushMessage> findAllByPushMessageIdIn(
            Collection<Long> pushMessageIds
    );

    @Query("""
            SELECT m.dispatch.pushDispatchId AS dispatchId,
                   m.status AS status,
                   COUNT(m) AS count
            FROM PushMessage m
            WHERE m.dispatch.pushDispatchId IN :dispatchIds
            GROUP BY m.dispatch.pushDispatchId, m.status
            """)
    List<PushDispatchMessageStatusCount> countStatusesByDispatchIds(
            @Param("dispatchIds") Collection<Long> dispatchIds
    );

    interface PushDispatchMessageStatusCount {
        Long getDispatchId();

        PushMessageStatus getStatus();

        long getCount();
    }
}
