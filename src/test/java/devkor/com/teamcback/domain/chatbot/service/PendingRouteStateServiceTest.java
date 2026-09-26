package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.dto.PendingLocationRef;
import devkor.com.teamcback.domain.chatbot.dto.PendingRouteState;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class PendingRouteStateServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> values;

    @Test
    void storesTypedStateInSeparateShortLivedKey() {
        ChatbotProperties properties = new ChatbotProperties(true, null,
                new ChatbotProperties.Agent(6, 5, 60, 15), null, null);
        when(redisTemplate.opsForValue()).thenReturn(values);
        PendingRouteStateService service = new PendingRouteStateService(redisTemplate, new ObjectMapper(), properties);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        PendingRouteState state = new PendingRouteState(RouteIntent.NAVIGATE_ROUTE,
                new PendingLocationRef(RouteEndpointType.BUILDING, 1L, "start"), null,
                SearchCampusRole.END, List.of(), List.of());

        service.save(sessionId, caller, state);

        verify(values).set(eq("chatbot:pending:route:" + sessionId), any(String.class), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void ownerMismatchDoesNotReturnPendingState() throws Exception {
        ChatbotProperties properties = new ChatbotProperties(true, null,
                new ChatbotProperties.Agent(6, 5, 60, 15), null, null);
        when(redisTemplate.opsForValue()).thenReturn(values);
        UUID sessionId = UUID.randomUUID();
        PendingRouteStateService service = new PendingRouteStateService(redisTemplate, new ObjectMapper(), properties);
        String stored = new ObjectMapper().writeValueAsString(new PendingRouteStateService.StoredPendingRoute(
                "another-owner", new PendingRouteState(RouteIntent.NAVIGATE_ROUTE, null, null,
                        SearchCampusRole.END, List.of(), List.of())));
        when(values.get("chatbot:pending:route:" + sessionId)).thenReturn(stored);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.load(sessionId,
                ChatCaller.from(null, "127.0.0.1")))
                .isInstanceOf(devkor.com.teamcback.global.exception.exception.GlobalException.class);
    }
}
