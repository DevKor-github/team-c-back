package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatServicePromptTest {
    @Test
    void keepsGeneralPromptMinimalAndGrounded() {
        assertThat(ChatService.SYSTEM_PROMPT)
                .contains("general conversation", "Dynamic campus facts", "do not guess")
                .doesNotContain("searchCampus", "findRoute", "Tool", "ROUTE_BEHAVIOR");
    }

    @Test
    void generalPromptDoesNotExposeWorkflowOrchestration() {
        assertThat(ChatService.SYSTEM_PROMPT)
                .doesNotContain("START", "END", "NAVIGATE_ROUTE", "TEXT_ROUTE", "six-call limit")
                .contains("private data", "internal identifiers");
    }
}
