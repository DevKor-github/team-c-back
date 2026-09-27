package devkor.com.teamcback.domain.notification.outbox;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name={"push.outbox.enabled"},havingValue="true")
public class PushDomainEventScheduler {
    private final PushDomainEventDelivery delivery;
    @Scheduled(fixedDelayString="${push.outbox.poll-interval-ms:1000}")
    public void run() { delivery.deliverOne(); }
}
