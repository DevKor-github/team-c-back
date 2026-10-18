package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.PlaceReviewsPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class PlaceReviewsPlanner {
    private final LlmGateway gateway;

    public PlaceReviewsPlanner(LlmGateway gateway) { this.gateway = gateway; }

    public PlaceReviewsPlan plan(List<LlmGateway.ConversationMessage> history, String message) {
        return gateway.planPlaceReviews("PLACE_REVIEWS workflow planner", history, message);
    }
}
