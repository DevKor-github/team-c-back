package devkor.com.teamcback.domain.chatbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.request.ChatMessageReq;
import devkor.com.teamcback.domain.chatbot.dto.PendingRouteState;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceRoutingTest {
    @Mock ChatOrchestrator chatOrchestrator;
    @Mock ChatSessionMemoryService memoryService;
    @Mock ChatRateLimiter rateLimiter;
    @Mock PendingRouteStateService pendingRouteStateService;
    @Mock CrowdStatusWorkflow crowdStatusWorkflow;
    @Mock MenuWorkflow menuWorkflow;

    private final UUID sessionId = UUID.randomUUID();
    private final ChatCaller caller = ChatCaller.from(null, "127.0.0.1");

    @BeforeEach
    void setUp() {
        when(memoryService.load(sessionId, caller)).thenReturn(List.of());
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(Optional.empty());
        lenient().when(chatOrchestrator.execute(anyString(), anyList(), anyString()))
                .thenReturn(new ChatOrchestrationResult("general", List.of()));
        lenient().when(chatOrchestrator.executeGeneral(anyString(), anyList(), anyString()))
                .thenReturn(new ChatOrchestrationResult("general", List.of()));
    }

    @Test
    void crowdPendingCandidateReplyContinuesCrowd() {
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(true);
        when(crowdStatusWorkflow.handle(any(), any(), anyList(), anyString()))
                .thenReturn(new CrowdStatusWorkflow.CrowdWorkflowResult(true, "crowd", true));

        newService().sendMessage(new ChatMessageReq(sessionId, "커피 파는 데", null), caller);

        verify(crowdStatusWorkflow).handle(sessionId, caller, List.of(), "커피 파는 데");
        verify(crowdStatusWorkflow, never()).cancel(any(), any());
        verify(chatOrchestrator, never()).execute(anyString(), anyList(), anyString());
    }

    @Test
    void crowdPendingMenuMessageCancelsCrowdAndUsesGeneralPath() {
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(true);
        when(menuWorkflow.handle(any(), any(), anyList(), anyString()))
                .thenReturn(new MenuWorkflow.WorkflowResult(true, "menu", false));

        newService().sendMessage(new ChatMessageReq(sessionId, "오늘 학식 뭐야", null), caller);

        verify(crowdStatusWorkflow).cancel(sessionId, caller);
        verify(crowdStatusWorkflow, never()).handle(any(), any(), anyList(), anyString());
        verify(menuWorkflow).handle(any(), any(), anyList(), anyString());
        verify(chatOrchestrator, never()).execute(anyString(), anyList(), anyString());
        verify(chatOrchestrator, never()).executeGeneral(anyString(), anyList(), anyString());
    }

    @Test
    void crowdPendingRouteMessageCancelsCrowdAndUsesRoutePath() {
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(true);

        newService().sendMessage(new ChatMessageReq(sessionId,
                "중앙도서관에서 서관까지 가는 길", null), caller);

        verify(crowdStatusWorkflow).cancel(sessionId, caller);
        verify(crowdStatusWorkflow, never()).handle(any(), any(), anyList(), anyString());
        verify(chatOrchestrator).execute(anyString(), anyList(), anyString());
    }

    @Test
    void routePendingCrowdMessageDeletesRoutePendingAndDoesNotResumeRoute() {
        when(pendingRouteStateService.load(sessionId, caller)).thenReturn(Optional.of(new PendingRouteState(
                devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent.NAVIGATE_ROUTE,
                null, null, devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole.END,
                List.of(), List.of())));
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(false);
        when(crowdStatusWorkflow.handle(any(), any(), anyList(), anyString()))
                .thenReturn(new CrowdStatusWorkflow.CrowdWorkflowResult(true, "crowd", false));

        newService().sendMessage(new ChatMessageReq(sessionId, "SK미래관 혼잡도 알려줘", null), caller);

        verify(pendingRouteStateService).delete(sessionId, caller);
        verify(crowdStatusWorkflow).handle(sessionId, caller, List.of(), "SK미래관 혼잡도 알려줘");
        verify(chatOrchestrator, never()).execute(anyString(), anyList(), anyString());
    }

    @Test
    void noPendingGreetingUsesGeneralPath() {
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(false);

        newService().sendMessage(new ChatMessageReq(sessionId, "안녕", null), caller);

        verify(crowdStatusWorkflow, never()).handle(any(), any(), anyList(), anyString());
        verify(chatOrchestrator).executeGeneral(anyString(), anyList(), anyString());
    }

    @Test
    void factualWorkflowFailureDoesNotFallBackToGeneralLlm() {
        when(crowdStatusWorkflow.hasPending(sessionId, caller)).thenReturn(false);
        when(menuWorkflow.handle(any(), any(), anyList(), anyString()))
                .thenReturn(new MenuWorkflow.WorkflowResult(false, null, false));

        var response = newService().sendMessage(
                new ChatMessageReq(sessionId, "오늘 학생회관 학식", null), caller);

        org.assertj.core.api.Assertions.assertThat(response.reply())
                .contains("캠퍼스 정보를 확인하지 못했어요");
        verify(menuWorkflow).handle(any(), any(), anyList(), anyString());
        verify(chatOrchestrator, never()).execute(anyString(), anyList(), anyString());
        verify(chatOrchestrator, never()).executeGeneral(anyString(), anyList(), anyString());
    }

    private ChatService newService() {
        return new ChatService(chatOrchestrator, memoryService, rateLimiter,
                pendingRouteStateService, crowdStatusWorkflow, new ChatRequestRouter(),
                null, null, null, menuWorkflow, null, null);
    }
}
