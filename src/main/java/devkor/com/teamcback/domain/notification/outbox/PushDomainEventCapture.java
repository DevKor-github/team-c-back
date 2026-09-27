package devkor.com.teamcback.domain.notification.outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.time.*;

/** Written within the original business transaction. Neon outages cannot lose committed events. */
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="push.storage",havingValue="neon")
public class PushDomainEventCapture {
    private final PushDomainEventRepository events;
    private final ObjectMapper mapper;
    private final Clock clock;
    @Value("${push.audience:INTERNAL_TEST}") private String audience;
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void character(CharacterUnlockedEvent event) { capture("CHARACTER",event); }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void report(ReportResolvedEvent event) { capture("REPORT",event); }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void crowd(PlaceBecameVacantEvent event) { capture("CROWD",event); }
    private void capture(String type,Object event) {
        try { events.save(new PushDomainEvent(type,audience,mapper.writeValueAsString(event),LocalDateTime.now(clock))); }
        catch (com.fasterxml.jackson.core.JsonProcessingException error) { throw new IllegalStateException("Unable to persist domain notification event",error); }
    }
}
