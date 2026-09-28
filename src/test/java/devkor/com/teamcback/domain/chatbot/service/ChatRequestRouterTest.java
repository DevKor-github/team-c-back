package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import devkor.com.teamcback.domain.chatbot.service.ChatRequestRouter.PendingWorkflow;
import devkor.com.teamcback.domain.chatbot.service.ChatRequestRouter.Route;
import devkor.com.teamcback.domain.chatbot.service.ChatRequestRouter.WorkflowType;
import org.junit.jupiter.api.Test;

class ChatRequestRouterTest {
    private final ChatRequestRouter router = new ChatRequestRouter();

    @Test
    void continuesCrowdForCandidateClarification() {
        var decision = router.route(PendingWorkflow.CROWD, "커피 파는 데");

        assertThat(decision.route()).isEqualTo(Route.CONTINUE_PENDING);
        assertThat(decision.workflowType()).isEqualTo(WorkflowType.CROWD);
    }

    @Test
    void doesNotResumeCrowdForMenuIntent() {
        var decision = router.route(PendingWorkflow.CROWD, "오늘 학식 뭐야");

        assertThat(decision.route()).isEqualTo(Route.NEW_INTENT);
        assertThat(decision.workflowType()).isEqualTo(WorkflowType.MENU);
    }

    @Test
    void doesNotResumeCrowdForRouteIntent() {
        var decision = router.route(PendingWorkflow.CROWD, "중앙도서관에서 서관까지 가는 길");

        assertThat(decision.route()).isEqualTo(Route.NEW_INTENT);
        assertThat(decision.workflowType()).isEqualTo(WorkflowType.ROUTE);
    }

    @Test
    void continuesRouteForClarificationButSwitchesToCrowdForCrowdIntent() {
        assertThat(router.route(PendingWorkflow.ROUTE, "1층 라운지").route())
                .isEqualTo(Route.CONTINUE_PENDING);
        var crowd = router.route(PendingWorkflow.ROUTE, "SK미래관 혼잡도 알려줘");
        assertThat(crowd.route()).isEqualTo(Route.NEW_INTENT);
        assertThat(crowd.workflowType()).isEqualTo(WorkflowType.CROWD);
    }

    @Test
    void routesGreetingWithoutPendingToGeneralChat() {
        var decision = router.route(PendingWorkflow.NONE, "안녕");

        assertThat(decision.route()).isEqualTo(Route.GENERAL_CHAT);
        assertThat(decision.workflowType()).isEqualTo(WorkflowType.GENERAL_CHAT);
    }

    @Test
    void startsExistingCrowdWorkflowForNewCrowdIntent() {
        var decision = router.route(PendingWorkflow.NONE, "지금 과학도서관 혼잡도 알려줘");

        assertThat(decision.route()).isEqualTo(Route.NEW_INTENT);
        assertThat(decision.workflowType()).isEqualTo(WorkflowType.CROWD);
    }
}
