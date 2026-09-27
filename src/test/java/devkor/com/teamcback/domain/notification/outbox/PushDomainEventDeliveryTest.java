package devkor.com.teamcback.domain.notification.outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent;
import devkor.com.teamcback.domain.notification.listener.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushDomainEventDeliveryTest {
    @Test void failedDeliveryRemainsPendingAndSuccessfulRetryCompletesSameEvent() throws Exception {
        var repository=mock(PushDomainEventRepository.class);
        var character=mock(CharacterUnlockedPushEventListener.class);
        var mapper=new ObjectMapper();
        var now=LocalDateTime.of(2026,9,28,12,0);
        var event=new PushDomainEvent("CHARACTER","INTERNAL_TEST",mapper.writeValueAsString(new CharacterUnlockedEvent(1L,2L,3L,"테스트")),now);
        var delivery=new PushDomainEventDelivery(repository,mapper,Clock.fixed(now.toInstant(ZoneOffset.UTC),ZoneOffset.UTC),character,
                mock(ReportResolvedPushEventListener.class),mock(CrowdVacantPushEventListener.class));
        org.springframework.test.util.ReflectionTestUtils.setField(delivery,"audience","INTERNAL_TEST");
        when(repository.claim("INTERNAL_TEST",now)).thenReturn(List.of(event));
        doThrow(new IllegalStateException("simulated unavailable store")).doNothing().when(character).handle(any());
        delivery.deliverOne();
        assertThat(event.getCompletedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(now);
        delivery.deliverOne();
        assertThat(event.getCompletedAt()).isEqualTo(now);
        verify(character,times(2)).handle(any());
    }
}
