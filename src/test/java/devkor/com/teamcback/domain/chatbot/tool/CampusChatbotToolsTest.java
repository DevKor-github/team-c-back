package devkor.com.teamcback.domain.chatbot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.ToolCallback;

class CampusChatbotToolsTest {
    @Test
    void registersExactlyNineApprovedTools() {
        assertThat(Arrays.stream(CampusChatbotTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(method -> method.getAnnotation(Tool.class).name()))
                .containsExactlyInAnyOrder("searchCampus", "getLocationDetail", "findFacilities", "findRoute",
                        "getCafeteriaMenu", "getRoomCourses", "getCampusStatus", "getCrowdStatus",
                        "getPlaceReviews");
    }

    @Test
    void campusStatusHasNoToolInput() throws NoSuchMethodException {
        assertThat(CampusChatbotTools.class.getDeclaredMethod("getCampusStatus").getParameterCount()).isZero();
    }

    @Test
    void descriptionsDistinguishResolverAndUiRouteActionFromTextRoute() throws NoSuchMethodException {
        String searchDescription = CampusChatbotTools.class
                .getDeclaredMethod("searchCampus",
                        devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest.class)
                .getAnnotation(Tool.class).description();
        String routeDescription = CampusChatbotTools.class
                .getDeclaredMethod("findRoute",
                        devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest.class)
                .getAnnotation(Tool.class).description();

        assertThat(searchDescription).contains("role START or END", "NAVIGATE_ROUTE", "never invent or select");
        assertThat(routeDescription).contains("textual route", "Do not call this only to open the UI route screen",
                "NAVIGATE_ROUTE");
    }

    @Test
    void generatesSearchCampusSchemaForRequestLocalTool() {
        ToolCallback callback = Arrays.stream(ToolCallbacks.from(new CampusChatbotTools(null, null)))
                .filter(candidate -> candidate.getToolDefinition().name().equals("searchCampus"))
                .findFirst()
                .orElseThrow();

        assertThat(callback.getToolDefinition().inputSchema())
                .contains("\"query\"", "\"limit\"", "\"role\"", "\"intent\"",
                        "NAVIGATE_ROUTE", "START", "END");
    }

    @Test
    void malformedSearchCampusEnumFailsBeforeToolMethodInvocation() {
        ToolCallback callback = Arrays.stream(ToolCallbacks.from(new CampusChatbotTools(null, null)))
                .filter(candidate -> candidate.getToolDefinition().name().equals("searchCampus"))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> callback.call(
                "{\"request\":{\"query\":\"x\",\"limit\":1,"
                        + "\"role\":\"INVALID\",\"intent\":\"NAVIGATE_ROUTE\",\"conditions\":[]}}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conversion from JSON");
    }
}
