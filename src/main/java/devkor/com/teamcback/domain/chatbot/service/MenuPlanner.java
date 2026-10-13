package devkor.com.teamcback.domain.chatbot.service;
import devkor.com.teamcback.domain.chatbot.dto.MenuPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix="chatbot", name="enabled", havingValue="true")
public class MenuPlanner {
    private final LlmGateway gateway;
    public MenuPlanner(LlmGateway gateway) { this.gateway = gateway; }
    public MenuPlan plan(List<LlmGateway.ConversationMessage> history, String message) {
        return gateway.planMenu("MENU: cafeteria meal lookup only; extract cafeteriaQuery and dateExpression", history, message);
    }
}
