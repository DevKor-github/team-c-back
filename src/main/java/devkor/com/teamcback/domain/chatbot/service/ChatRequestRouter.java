package devkor.com.teamcback.domain.chatbot.service;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Small application boundary that decides whether a pending interaction owns the current message.
 * It intentionally does not call an LLM or inspect domain identifiers.
 */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatRequestRouter {
    public RoutingDecision route(PendingWorkflow pendingWorkflow, String userMessage) {
        Intent workflowIntent = detectObviousIntent(userMessage);

        if (pendingWorkflow != null && pendingWorkflow != PendingWorkflow.NONE) {
            if (isGeneralConversation(userMessage)) {
                return RoutingDecision.generalChat();
            }
            if (workflowIntent != Intent.NONE && !matchesPending(pendingWorkflow, workflowIntent)) {
                return RoutingDecision.newIntent(workflowIntent);
            }
            return RoutingDecision.continuePending(pendingWorkflow.toWorkflowType());
        }

        if (workflowIntent == Intent.NONE) {
            return RoutingDecision.generalChat();
        }
        return RoutingDecision.newIntent(workflowIntent);
    }

    private boolean matchesPending(PendingWorkflow pendingWorkflow, Intent intent) {
        return (pendingWorkflow == PendingWorkflow.CROWD && intent == Intent.CROWD)
                || (pendingWorkflow == PendingWorkflow.ROUTE && intent == Intent.ROUTE)
                || (pendingWorkflow == PendingWorkflow.LOCATION_DETAIL && intent == Intent.LOCATION_DETAIL)
                || (pendingWorkflow == PendingWorkflow.REVIEW && intent == Intent.REVIEW)
                || (pendingWorkflow == PendingWorkflow.MENU && intent == Intent.MENU)
                || (pendingWorkflow == PendingWorkflow.FACILITY && intent == Intent.FACILITY)
                || (pendingWorkflow == PendingWorkflow.ROOM_COURSE && intent == Intent.ROOM_COURSE);
    }

    private Intent detectObviousIntent(String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return Intent.NONE;
        }
        if (normalized.contains("route")) {
            return Intent.ROUTE;
        }
        if (containsAny(normalized, "길찾", "가는길", "경로", "어디로가", "길알려줘", "어떻게가", "길안내", "도보로")) {
            return Intent.ROUTE;
        }
        if (containsAny(normalized, "학식", "식단")) {
            return Intent.MENU;
        }
        if (containsAny(normalized, "붐비", "혼잡", "사람많", "crowd")) {
            return Intent.CROWD;
        }
        if (containsAny(normalized, "리뷰", "후기", "평어때", "평가")) {
            return Intent.REVIEW;
        }
        if (containsAny(normalized, "수업", "강의", "시간표")) {
            return Intent.ROOM_COURSE;
        }
        if (containsAny(normalized, "화장실", "프린터", "시설", "자판기", "정수기")) {
            return Intent.FACILITY;
        }
        if (containsAny(normalized, "방학", "학기", "고연전", "학교상태", "학교현황", "오늘학교", "지금학교", "캠퍼스상태")) {
            return Intent.CAMPUS_STATUS;
        }
        if (containsAny(normalized, "어디야", "어디있", "열려", "운영시간", "몇시", "닫", "정보")) {
            return Intent.LOCATION_DETAIL;
        }
        return Intent.NONE;
    }

    private boolean isGeneralConversation(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "안녕", "hello", "hi", "고마워", "감사", "잘자");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public enum PendingWorkflow {
        NONE, MENU, FACILITY, ROOM_COURSE,
        ROUTE,
        CROWD,
        LOCATION_DETAIL,
        REVIEW;

        private WorkflowType toWorkflowType() {
            return switch (this) {
                case CROWD -> WorkflowType.CROWD;
                case LOCATION_DETAIL -> WorkflowType.LOCATION_DETAIL;
                case REVIEW -> WorkflowType.REVIEW;
                case ROUTE -> WorkflowType.ROUTE;
                case MENU -> WorkflowType.MENU;
                case FACILITY -> WorkflowType.FACILITY;
                case ROOM_COURSE -> WorkflowType.ROOM_COURSE;
                case NONE -> WorkflowType.GENERAL_CHAT;
            };
        }
    }

    public enum Route {
        CONTINUE_PENDING,
        NEW_INTENT,
        GENERAL_CHAT
    }

    public enum WorkflowType {
        ROUTE,
        CROWD,
        MENU,
        ROOM_COURSE,
        REVIEW,
        FACILITY,
        LOCATION_DETAIL,
        CAMPUS_STATUS,
        GENERAL_CHAT
    }

    public enum Intent {
        ROUTE,
        CROWD,
        MENU,
        ROOM_COURSE,
        REVIEW,
        FACILITY,
        LOCATION_DETAIL,
        CAMPUS_STATUS,
        NONE;

        private WorkflowType toWorkflowType() {
            return switch (this) {
                case ROUTE -> WorkflowType.ROUTE;
                case CROWD -> WorkflowType.CROWD;
                case MENU -> WorkflowType.MENU;
                case ROOM_COURSE -> WorkflowType.ROOM_COURSE;
                case REVIEW -> WorkflowType.REVIEW;
                case FACILITY -> WorkflowType.FACILITY;
                case LOCATION_DETAIL -> WorkflowType.LOCATION_DETAIL;
                case CAMPUS_STATUS -> WorkflowType.CAMPUS_STATUS;
                case NONE -> WorkflowType.GENERAL_CHAT;
            };
        }
    }

    public record RoutingDecision(Route route, WorkflowType workflowType) {
        public RoutingDecision {
            route = route == null ? Route.GENERAL_CHAT : route;
            workflowType = workflowType == null ? WorkflowType.GENERAL_CHAT : workflowType;
        }

        public static RoutingDecision continuePending(WorkflowType workflowType) {
            return new RoutingDecision(Route.CONTINUE_PENDING, workflowType);
        }

        public static RoutingDecision newIntent(Intent intent) {
            return new RoutingDecision(Route.NEW_INTENT, intent.toWorkflowType());
        }

        public static RoutingDecision generalChat() {
            return new RoutingDecision(Route.GENERAL_CHAT, WorkflowType.GENERAL_CHAT);
        }
    }
}
