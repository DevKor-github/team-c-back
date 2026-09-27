package devkor.com.teamcback.domain.chatbot.gateway;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import devkor.com.teamcback.domain.chatbot.tool.CampusChatbotTools;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusMatchType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.mockito.ArgumentCaptor;

class SpringAiLlmGatewayTest {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void usesStructuredOutputWithoutRegisteringCampusToolsForRoutePlanning() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(any(StructuredOutputConverter.class))).thenReturn(
                new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "start", "end", List.of()));
        SpringAiLlmGateway gateway = new SpringAiLlmGateway(builder, properties(), executor,
                mock(CampusChatbotTools.class), new ChatbotToolCallLimiter(properties()));

        RoutePlan plan = gateway.planRoute("system", List.of(), "route");

        assertThat(plan.intent()).isEqualTo(RoutePlan.Intent.TEXT_ROUTE);
        assertThat(plan.startQuery()).isEqualTo("start");
        verify(requestSpec).call();
        org.mockito.Mockito.verify(requestSpec, org.mockito.Mockito.never()).tools(any());
    }

    @Test
    void mapsProviderFailureWithoutExposingProviderDetails() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        CampusChatbotTools tools = mock(CampusChatbotTools.class);
        when(builder.build()).thenReturn(chatClient);
        when(tools.forRequest(any(), any())).thenReturn(tools);
        when(chatClient.prompt().system("system").messages(anyList()).tools(any(CampusChatbotTools.class)).call().content())
                .thenThrow(new IllegalStateException("provider secret and internal details"));
        SpringAiLlmGateway gateway = new SpringAiLlmGateway(builder, properties(), executor, tools,
                new ChatbotToolCallLimiter(properties()));

        assertThatThrownBy(() -> gateway.generate("system", List.of(), "hello",
                new ResolvedLocationCollector()))
                .isInstanceOfSatisfying(GlobalException.class, exception -> {
                    assertThat(exception.getResultCode()).isEqualTo(CHATBOT_TEMPORARILY_UNAVAILABLE);
                    assertThat(exception.getMessage()).isNull();
                    assertThat(exception.getCause()).isNull();
                });
    }

    @Test
    void sendsHistoryAsOrderedRoleMessagesAndRegistersRequestLocalTools() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        CampusChatbotTools tools = mock(CampusChatbotTools.class);
        CampusChatbotTools requestTools = mock(CampusChatbotTools.class);
        when(builder.build()).thenReturn(chatClient);
        when(tools.forRequest(any(), any())).thenReturn(requestTools);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system("system")).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.tools(requestTools)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage("done")))), java.util.Map.of()));
        SpringAiLlmGateway gateway = new SpringAiLlmGateway(builder, properties(), executor, tools,
                new ChatbotToolCallLimiter(properties()));

        LlmGateway.LlmResult result = gateway.generate("system", List.of(
                new LlmGateway.ConversationMessage(LlmGateway.Role.USER, "old user"),
                new LlmGateway.ConversationMessage(LlmGateway.Role.ASSISTANT, "old assistant")), "current user",
                new ResolvedLocationCollector());

        assertThat(result.reply()).isEqualTo("done");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> messages = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(messages.capture());
        assertThat(messages.getValue()).hasSize(3);
        assertThat(messages.getValue().get(0)).isInstanceOf(UserMessage.class);
        assertThat(messages.getValue().get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.getValue().get(2)).isInstanceOf(UserMessage.class);
        verify(requestSpec).tools(requestTools);
    }

    @Test
    void preservesRequestLocalToolStateWhenFinalModelFollowUpFails() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        CampusToolAdapter adapter = mock(CampusToolAdapter.class);
        ChatbotProperties properties = properties();
        ChatbotToolCallLimiter limiter = new ChatbotToolCallLimiter(properties);
        CampusChatbotTools baseTools = new CampusChatbotTools(adapter, limiter);
        AtomicReference<CampusChatbotTools> requestTools = new AtomicReference<>();
        SearchCampusToolRequest startRequest = new SearchCampusToolRequest("start", 1,
                SearchCampusRole.START, SearchCampusIntent.NAVIGATE_ROUTE, List.of());
        SearchCampusToolRequest endRequest = new SearchCampusToolRequest("end", 1,
                SearchCampusRole.END, SearchCampusIntent.NAVIGATE_ROUTE, List.of());
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system("system")).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.tools(any(CampusChatbotTools.class))).thenAnswer(invocation -> {
            requestTools.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(requestSpec.call()).thenReturn(responseSpec);
        when(adapter.searchCampus(startRequest)).thenReturn(resolved(11L, "start"));
        when(adapter.searchCampus(endRequest)).thenReturn(resolved(22L, "end"));
        when(responseSpec.chatClientResponse()).thenAnswer(invocation -> {
            requestTools.get().searchCampus(startRequest);
            requestTools.get().searchCampus(endRequest);
            return new ChatClientResponse(
                    new ChatResponse(List.of(new Generation(new AssistantMessage("")))), java.util.Map.of());
        });
        SpringAiLlmGateway gateway = new SpringAiLlmGateway(builder, properties, executor, baseTools, limiter);

        ResolvedLocationCollector executionState = new ResolvedLocationCollector();
        LlmGateway.LlmResult result = gateway.generate("system", List.of(), "route request", executionState);

        assertThat(result.reply()).isNull();
        assertThat(result.completionStatus())
                .isEqualTo(LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION);
        assertThat(executionState.snapshot()).hasSize(2);
        assertThat(executionState.snapshot()).extracting(location -> location.id())
                .containsExactly(11L, 22L);
    }

    private SearchCampusToolResult resolved(long id, String name) {
        return new SearchCampusToolResult(List.of(new SearchCampusItem(
                id, ToolLocationType.BUILDING, name, id, name, null, null, null,
                SearchCampusMatchType.EXACT)), false, null);
    }

    private ChatbotProperties properties() {
        return new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60, 15),
                new ChatbotProperties.Tools(
                        new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
    }
}
