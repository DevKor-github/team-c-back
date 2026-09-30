package devkor.com.teamcback.domain.chatbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

class ChatbotToolExecutionConfigTest {
    @Test
    void convertsToolExecutionFailureToValidJsonObject() throws Exception {
        ToolDefinition definition = ToolDefinition.builder()
                .name("searchCampus").description("test").inputSchema("{}").build();
        ToolExecutionException exception = new ToolExecutionException(definition,
                new IllegalArgumentException("Cannot bind tool arguments"));

        String response = new ChatbotToolExecutionConfig()
                .chatbotToolExecutionExceptionProcessor().process(exception);

        assertThat(response).isEqualTo("{\"success\":false,\"error\":\"TOOL_EXECUTION_FAILED\"}");
        assertThat(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("success").asBoolean()).isFalse();
    }
}
