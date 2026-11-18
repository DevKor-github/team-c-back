package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutePlannerTest {
    @Mock LlmGateway llmGateway;

    @Test
    void preservesStructuredRoutePlanWithoutResolvingIds() {
        RoutePlan expected = new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "중앙도서관(신관)", "문과대학(서관)", List.of());
        when(llmGateway.planRoute(anyString(), anyList(), anyString())).thenReturn(expected);

        RoutePlan actual = new RoutePlanner(llmGateway).plan("system", List.of(), "route");

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.startQuery()).isEqualTo("중앙도서관(신관)");
        assertThat(actual.endQuery()).isEqualTo("문과대학(서관)");
    }

    @Test
    void invalidStructuredRoutePlanFallsBackToNotRoute() {
        when(llmGateway.planRoute(anyString(), anyList(), anyString()))
                .thenReturn(new RoutePlan(RoutePlan.Intent.NAVIGATE_ROUTE, "", "end", List.of()));

        RoutePlan actual = new RoutePlanner(llmGateway).plan("system", List.of(), "route");

        assertThat(actual.intent()).isEqualTo(RoutePlan.Intent.NOT_ROUTE);
    }
}
