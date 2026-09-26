package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatServicePromptTest {
    @Test
    void separatesUiActionAndTextRouteOrchestration() {
        assertThat(ChatService.SYSTEM_PROMPT)
                .contains("[ROUTE_BEHAVIOR]", "intent=NAVIGATE_ROUTE", "do not call findRoute")
                .contains("intent=TEXT_ROUTE", "then findRoute")
                .contains("backend creates", "actual Tool results")
                .contains("never invent", "ambiguous", "PENDING_ROUTE_CONTINUATION", "never copy");
    }

    @Test
    void routeContractKeepsAllowedRolesAndIntentsExplicit() {
        assertThat(ChatService.SYSTEM_PROMPT)
                .contains("role=START", "role=END", "START/END", "NAVIGATE_ROUTE/TEXT_ROUTE")
                .contains("BARRIERFREE", "six-call limit", "latest user");
    }
}
