package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;

class DomainPushEventListenerAnnotationTest {

    @Test
    void listenersRunAfterCommit() throws NoSuchMethodException {
        assertAfterCommit(CrowdVacantPushEventListener.class, PlaceBecameVacantEvent.class);
        assertAfterCommit(ReportResolvedPushEventListener.class, ReportResolvedEvent.class);
        assertAfterCommit(CharacterUnlockedPushEventListener.class, CharacterUnlockedEvent.class);
    }

    private void assertAfterCommit(
            Class<?> listenerClass,
            Class<?> eventClass
    ) throws NoSuchMethodException {
        TransactionalEventListener annotation = listenerClass
                .getDeclaredMethod("handle", eventClass)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
