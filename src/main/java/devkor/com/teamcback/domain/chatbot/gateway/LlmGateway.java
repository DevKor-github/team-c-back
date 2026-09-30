package devkor.com.teamcback.domain.chatbot.gateway;

import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.dto.CrowdStatusPlan;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateView;
import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateView;
import devkor.com.teamcback.domain.chatbot.dto.LocationDetailPlan;
import devkor.com.teamcback.domain.chatbot.dto.PlaceReviewsPlan;
import devkor.com.teamcback.domain.chatbot.dto.MenuPlan;
import devkor.com.teamcback.domain.chatbot.dto.FacilityPlan;
import devkor.com.teamcback.domain.chatbot.dto.RoomCoursePlan;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import java.util.List;

public interface LlmGateway {
    /** Structured intent/slot extraction for the isolated crowd-status POC. */
    default CrowdStatusPlan planCrowd(String systemPrompt, List<ConversationMessage> history, String userMessage) {
        return CrowdStatusPlan.other();
    }
    /** Structured semantic selection over backend-owned candidates; implementations must not execute tools. */
    default CrowdCandidateSelection selectCrowdCandidate(String systemPrompt,
                                                          List<ConversationMessage> history,
                                                          String userMessage,
                                                          List<CrowdCandidateView> candidates) {
        return CrowdCandidateSelection.none();
    }
    /** Structured route interpretation; provider implementations must not resolve IDs or execute routes. */
    default RoutePlan planRoute(String systemPrompt, List<ConversationMessage> history, String userMessage) {
        return RoutePlan.notRoute();
    }
    default LocationDetailPlan planLocationDetail(String systemPrompt, List<ConversationMessage> history,
                                                   String userMessage) {
        return LocationDetailPlan.other();
    }
    default PlaceReviewsPlan planPlaceReviews(String systemPrompt, List<ConversationMessage> history,
                                               String userMessage) {
        return PlaceReviewsPlan.other();
    }
    default MenuPlan planMenu(String systemPrompt, List<ConversationMessage> history, String userMessage) { return MenuPlan.other(); }
    default FacilityPlan planFacility(String systemPrompt, List<ConversationMessage> history, String userMessage) { return FacilityPlan.other(); }
    default RoomCoursePlan planRoomCourse(String systemPrompt, List<ConversationMessage> history, String userMessage) { return RoomCoursePlan.other(); }
    default LocationCandidateSelection selectLocationCandidate(String systemPrompt,
                                                                 List<ConversationMessage> history,
                                                                 String userMessage,
                                                                 List<LocationCandidateView> candidates) {
        return LocationCandidateSelection.none();
    }

    LlmResult generate(String systemPrompt, List<ConversationMessage> history, String userMessage,
                       ResolvedLocationCollector executionState);

    record LlmResult(String reply, CompletionStatus completionStatus) {
        public LlmResult(String reply) {
            this(reply, CompletionStatus.COMPLETE);
        }

        public LlmResult {
            completionStatus = completionStatus == null ? CompletionStatus.COMPLETE : completionStatus;
        }
    }

    enum CompletionStatus {
        COMPLETE,
        FAILED_AFTER_TOOL_EXECUTION
    }

    enum Role {
        USER,
        ASSISTANT
    }

    record ConversationMessage(Role role, String content) {
    }
}
