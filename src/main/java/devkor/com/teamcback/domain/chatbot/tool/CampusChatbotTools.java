package devkor.com.teamcback.domain.chatbot.tool;

import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CampusChatbotTools {
    private final CampusToolAdapter adapter;

    public CampusChatbotTools(CampusToolAdapter adapter) {
        this.adapter = adapter;
    }

    @Tool(name = "searchCampus", description = "장소 ID를 모를 때 가장 먼저 사용해 고려대학교 건물 또는 장소 이름 후보를 찾습니다.")
    public SearchCampusToolResult searchCampus(
            @ToolParam(description = "검색어와 선택적 결과 제한") SearchCampusToolRequest request) {
        return adapter.searchCampus(request);
    }

    @Tool(name = "getLocationDetail", description = "searchCampus로 확인한 BUILDING 또는 PLACE ID의 상세와 운영 정보를 조회합니다.")
    public GetLocationDetailToolResult getLocationDetail(
            @ToolParam(description = "BUILDING 또는 PLACE 유형과 위치 ID") GetLocationDetailToolRequest request) {
        return adapter.getLocationDetail(request);
    }

    @Tool(name = "findFacilities", description = "시설 유형이나 건물/층 조건으로 고려대학교 시설을 찾습니다. 층은 건물 ID와 함께 사용해야 합니다.")
    public FindFacilitiesToolResult findFacilities(
            @ToolParam(description = "시설 유형, 건물 ID, 층, 선택적 결과 제한") FindFacilitiesToolRequest request) {
        return adapter.findFacilities(request);
    }
}
