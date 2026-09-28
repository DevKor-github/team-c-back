package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.LocationDetailPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class LocationDetailPlanner {
    private final LlmGateway gateway;

    public LocationDetailPlanner(LlmGateway gateway) { this.gateway = gateway; }

    public LocationDetailPlan plan(List<LlmGateway.ConversationMessage> history, String message) {
        return gateway.planLocationDetail("LOCATION_DETAIL workflow planner", history, message);
    }
}
