package devkor.com.teamcback.domain.chatbot.service;
import devkor.com.teamcback.domain.chatbot.dto.RoomCoursePlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix="chatbot", name="enabled", havingValue="true")
public class RoomCoursePlanner {
    private final LlmGateway gateway;
    public RoomCoursePlanner(LlmGateway gateway) { this.gateway = gateway; }
    public RoomCoursePlan plan(List<LlmGateway.ConversationMessage> history, String message) {
        return gateway.planRoomCourse("ROOM_COURSE: classroom schedule only; extract roomQuery and weekdayExpression", history, message);
    }
}
