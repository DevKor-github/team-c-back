package devkor.com.teamcback.domain.chatbot.service;
import devkor.com.teamcback.domain.chatbot.dto.FacilityPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix="chatbot", name="enabled", havingValue="true")
public class FacilityPlanner {
    private final LlmGateway gateway;
    public FacilityPlanner(LlmGateway gateway) { this.gateway = gateway; }
    public FacilityPlan plan(List<LlmGateway.ConversationMessage> history, String message) {
        return gateway.planFacility("FACILITY: extract natural-language locationQuery, facilityType, and optional numeric floor", history, message);
    }
}
