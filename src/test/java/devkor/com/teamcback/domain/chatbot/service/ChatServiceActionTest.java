package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.PendingLocationRef;
import devkor.com.teamcback.domain.chatbot.dto.PendingRouteState;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolData;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.EndpointRole;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent;
import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceActionTest {
    @Mock LlmGateway llmGateway;
    @Mock ChatSessionMemoryService memoryService;
    @Mock ChatRateLimiter rateLimiter;
    @Mock PendingRouteStateService pendingRouteStateService;

    @Test
    void assemblesNavigateRouteOnlyFromResolvedToolReferences() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                "model invented id 999999", List.of(
                new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L,
                        "송현스퀘어", RouteIntent.NAVIGATE_ROUTE, List.of(RouteCondition.BARRIERFREE)),
                new ResolvedLocation(EndpointRole.END, RouteEndpointType.PLACE, 456L,
                        "중앙도서관", RouteIntent.NAVIGATE_ROUTE, List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "길찾기 해줘", null), caller);

        assertThat(response.action()).isNotNull();
        assertThat(response.action().payload().startId()).isEqualTo(123L);
        assertThat(response.action().payload().endId()).isEqualTo(456L);
        assertThat(response.action().payload().conditions()).containsExactly(RouteCondition.BARRIERFREE);
        assertThat(response.reply()).doesNotContain("999999");
    }

    @Test
    void doesNotAssembleActionForAmbiguousOrTextRouteTrace() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                "text route", List.of(new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L,
                        "학생회관", RouteIntent.TEXT_ROUTE, List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "몇 분 걸려?", null), caller);

        assertThat(response.action()).isNull();
        assertThat(response.reply()).isEqualTo("text route");
    }

    @Test
    void returnsValidatedActionWhenFinalModelFollowUpFailed() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(
                new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L,
                        "start", RouteIntent.NAVIGATE_ROUTE, List.of()),
                new ResolvedLocation(EndpointRole.END, RouteEndpointType.PLACE, 456L,
                        "end", RouteIntent.NAVIGATE_ROUTE, List.of())),
                LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller);

        assertThat(response.action()).isNotNull();
        assertThat(response.action().payload().startId()).isEqualTo(123L);
        assertThat(response.action().payload().endId()).isEqualTo(456L);
        assertThat(response.reply()).contains("start", "end");
    }

    @Test
    void keepsProviderFailureWhenFailedFollowUpHasOnlyOneEndpoint() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L,
                        "start", RouteIntent.NAVIGATE_ROUTE, List.of())),
                LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        assertThatThrownBy(() -> service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller))
                .isInstanceOf(devkor.com.teamcback.global.exception.exception.GlobalException.class)
                .satisfies(exception -> assertThat(
                        ((devkor.com.teamcback.global.exception.exception.GlobalException) exception).getResultCode())
                        .isEqualTo(devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE));
    }

    @Test
    void storesAmbiguousEndpointAsPendingRouteState() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                "어느 문과대학 서관인지 알려주세요", List.of(
                new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L, "송현스퀘어",
                        RouteIntent.NAVIGATE_ROUTE, List.of())), LlmGateway.CompletionStatus.COMPLETE,
                List.of(new SearchResolutionTrace(SearchCampusRole.START, SearchCampusIntent.NAVIGATE_ROUTE,
                                "송현스퀘어", false,
                                List.of(new PendingLocationRef(RouteEndpointType.BUILDING, 123L, "송현스퀘어")), List.of()),
                        new SearchResolutionTrace(SearchCampusRole.END, SearchCampusIntent.NAVIGATE_ROUTE,
                                "문과대학 서관", true,
                                List.of(new PendingLocationRef(RouteEndpointType.BUILDING, 201L, "문과대학 서관"),
                                        new PendingLocationRef(RouteEndpointType.PLACE, 202L, "문과대학 서관 1층 라운지")), List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(java.util.Optional.empty());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller);

        assertThat(response.action()).isNull();
        org.mockito.ArgumentCaptor<PendingRouteState> state = org.mockito.ArgumentCaptor.forClass(PendingRouteState.class);
        verify(pendingRouteStateService).save(eq(sessionId), eq(caller), state.capture());
        assertThat(state.getValue().unresolvedRole()).isEqualTo(SearchCampusRole.END);
        assertThat(state.getValue().resolvedStart().locationId()).isEqualTo(123L);
        assertThat(state.getValue().ambiguousCandidates()).hasSize(2);
    }

    @Test
    void createsActionOnlyFromCurrentTurnAfterPendingClarification() {
        PendingRouteState pending = new PendingRouteState(RouteIntent.NAVIGATE_ROUTE,
                new PendingLocationRef(RouteEndpointType.BUILDING, 999L, "old start"), null,
                SearchCampusRole.END, List.of(), List.of(new PendingLocationRef(RouteEndpointType.BUILDING, 201L, "old end")));
        when(pendingRouteStateService.load(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(pending));
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                "완료", List.of(
                new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L, "new start",
                        RouteIntent.NAVIGATE_ROUTE, List.of()),
                new ResolvedLocation(EndpointRole.END, RouteEndpointType.PLACE, 456L, "new end",
                        RouteIntent.NAVIGATE_ROUTE, List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "1층 라운지로 해줘", null), caller);

        assertThat(response.action()).isNotNull();
        assertThat(response.action().payload().startId()).isEqualTo(123L);
        assertThat(response.action().payload().endId()).isEqualTo(456L);
        verify(pendingRouteStateService).delete(sessionId, caller);
        org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(llmGateway).generate(prompt.capture(), anyList(), eq("1층 라운지로 해줘"));
        assertThat(prompt.getValue()).contains("PENDING_ROUTE_CONTINUATION", "interaction=NAVIGATE_ROUTE",
                "unresolvedRole=END", "re-search both endpoints", "never reuse an old ID");
    }

    @Test
    void returnsDeterministicClarificationWhenEndIsAmbiguousAndCompletionIsBlank() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L, "송현스퀘어",
                        RouteIntent.NAVIGATE_ROUTE, List.of())), LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                List.of(new SearchResolutionTrace(SearchCampusRole.END, SearchCampusIntent.NAVIGATE_ROUTE,
                        "중앙도서관", true,
                        List.of(new PendingLocationRef(RouteEndpointType.BUILDING, 201L, "중앙도서관(신관)"),
                                new PendingLocationRef(RouteEndpointType.PLACE, 202L, "중앙도서관(대학원)")), List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(java.util.Optional.empty());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller);

        assertThat(response.action()).isNull();
        assertThat(response.reply()).contains("중앙도서관(신관)", "중앙도서관(대학원)");
        verify(memoryService).save(eq(sessionId), eq(caller), eq("route"), org.mockito.ArgumentMatchers.contains("도착지"));
        verify(pendingRouteStateService).save(eq(sessionId), eq(caller), org.mockito.ArgumentMatchers.any(PendingRouteState.class));
    }

    @Test
    void returnsDeterministicClarificationWhenStartIsAmbiguousAndCompletionIsBlank() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                "", List.of(new ResolvedLocation(EndpointRole.END, RouteEndpointType.BUILDING, 456L, "중앙도서관",
                        RouteIntent.NAVIGATE_ROUTE, List.of())), LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                List.of(new SearchResolutionTrace(SearchCampusRole.START, SearchCampusIntent.NAVIGATE_ROUTE,
                        "학생회관", true,
                        List.of(new PendingLocationRef(RouteEndpointType.BUILDING, 101L, "학생회관 본관")), List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(java.util.Optional.empty());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller);

        assertThat(response.reply()).contains("출발지 후보가 여러 개 있어요", "학생회관 본관");
        assertThat(response.action()).isNull();
    }

    @Test
    void deduplicatesSameActualCandidateInDeterministicClarification() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(), LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                List.of(new SearchResolutionTrace(SearchCampusRole.START, SearchCampusIntent.NAVIGATE_ROUTE,
                        "중앙도서관", true,
                        List.of(new PendingLocationRef(RouteEndpointType.PLACE, 300L, "야외 중앙도서관 장애인주차장"),
                                new PendingLocationRef(RouteEndpointType.PLACE, 300L, "야외 중앙도서관 장애인주차장"),
                                new PendingLocationRef(RouteEndpointType.PLACE, 301L, "야외 중앙도서관 장애인주차장")), List.of()))));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(java.util.Optional.empty());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "route", null), caller);

        assertThat(response.reply()).isEqualTo("출발지 후보가 여러 개 있어요. 야외 중앙도서관 장애인주차장, 야외 중앙도서관 장애인주차장 중 어디로 갈까요?");
    }

    @Test
    void keepsTextRouteCompletionFailureAsError() {
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(), LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(new ChatMessageReq(sessionId, "text route", null), caller))
                .isInstanceOf(devkor.com.teamcback.global.exception.exception.GlobalException.class);
    }

    @Test
    void returnsDeterministicTextRouteReplyWhenFinalCompletionIsBlank() {
        ResolvedLocation start = new ResolvedLocation(EndpointRole.START, RouteEndpointType.BUILDING, 123L,
                "start", RouteIntent.TEXT_ROUTE, List.of());
        ResolvedLocation end = new ResolvedLocation(EndpointRole.END, RouteEndpointType.PLACE, 456L,
                "end", RouteIntent.TEXT_ROUTE, List.of());
        RouteExecutionTrace execution = new RouteExecutionTrace(start, end, List.of(),
                new FindRouteToolData(120L, List.of()), true);
        when(llmGateway.generate(anyString(), anyList(), anyString())).thenReturn(new LlmGateway.LlmResult(
                null, List.of(start, end), LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                List.of(), execution));
        ChatService service = new ChatService(llmGateway, memoryService, rateLimiter, pendingRouteStateService);
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());

        var response = service.sendMessage(new ChatMessageReq(sessionId, "몇 분 걸려?", null), caller);

        assertThat(response.action()).isNull();
        assertThat(response.reply()).isEqualTo("start에서 end까지 경로를 찾았습니다. 예상 소요 시간은 120초입니다.");
    }
}
