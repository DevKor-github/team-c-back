package devkor.com.teamcback.domain.notification.outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.report.event.ReportResolvedEvent;
import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import devkor.com.teamcback.domain.notification.listener.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="push.storage",havingValue="neon")
public class PushDomainEventDelivery {
    private final PushDomainEventRepository events;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final CharacterUnlockedPushEventListener character;
    private final ReportResolvedPushEventListener report;
    private final CrowdVacantPushEventListener crowd;
    @Value("${push.audience:INTERNAL_TEST}") private String audience;
    @Transactional("transactionManager")
    public void deliverOne() {
        var now=LocalDateTime.now(clock);
        for (var event:events.claim(audience,now)) {
            try {
                switch(event.getType()) {
                    case "CHARACTER" -> character.handle(mapper.readValue(event.getPayload(),CharacterUnlockedEvent.class));
                    case "REPORT" -> report.handle(mapper.readValue(event.getPayload(),ReportResolvedEvent.class));
                    case "CROWD" -> crowd.handle(mapper.readValue(event.getPayload(),PlaceBecameVacantEvent.class));
                    default -> throw new IllegalStateException("Unknown notification event type");
                }
                event.complete(now);
            } catch(Exception error) {
                event.retry(now);
                log.warn("Push domain event delivery deferred: id={}, attempts={}, errorType={}",event.getId(),event.getAttempts(),error.getClass().getSimpleName());
            }
        }
    }
}
