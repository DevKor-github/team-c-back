package devkor.com.teamcback.domain.chatbot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

class CampusChatbotToolsTest {
    @Test
    void registersExactlyTheThreePhaseTwoTools() {
        assertThat(Arrays.stream(CampusChatbotTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(method -> method.getAnnotation(Tool.class).name()))
                .containsExactlyInAnyOrder("searchCampus", "getLocationDetail", "findFacilities");
    }
}
