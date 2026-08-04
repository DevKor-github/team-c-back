package devkor.com.teamcback.domain.notification.repository;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class PushMessageRepositoryQueryTest {

    @Test
    void dueReceiptQueryTargetsOnlyReceiptPendingMessagesWithAvailableReceipts() throws Exception {
        Method method = PushMessageRepository.class.getMethod(
                "findDueReceiptPendingForUpdateSkipLocked",
                java.time.LocalDateTime.class,
                int.class
        );

        String query = method.getAnnotation(Query.class).value();

        assertThat(query).contains("status = 'RECEIPT_PENDING'");
        assertThat(query).contains("expo_ticket_id IS NOT NULL");
        assertThat(query).contains("receipt_available_at <= :now");
        assertThat(query).contains("FOR UPDATE SKIP LOCKED");
    }
}
