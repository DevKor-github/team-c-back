package devkor.com.teamcback.domain.chatbot.tool;

import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolResult;
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

    @Tool(name = "findRoute", description = "Find a campus route between BUILDING/PLACE IDs or request-scoped COORD coordinates. Use searchCampus first when an ID is unknown and never select an ambiguous candidate. BARRIERFREE only excludes stair nodes and is not a complete accessibility guarantee.")
    public FindRouteToolResult findRoute(
            @ToolParam(description = "Start, end, and optional BARRIERFREE/SHUTTLE/STUDENTCARD/OPERATING conditions") FindRouteToolRequest request) {
        return adapter.findRoute(request);
    }

    @Tool(name = "getCafeteriaMenu", description = "교내 식당의 지정 날짜 또는 최대 7일 메뉴를 조회합니다. 일반 카페 상품 메뉴 조회에는 사용하지 않습니다.")
    public GetCafeteriaMenuToolResult getCafeteriaMenu(
            @ToolParam(description = "식당 PLACE ID와 조회 시작일, 선택적 종료일") GetCafeteriaMenuToolRequest request) {
        return adapter.getCafeteriaMenu(request);
    }

    @Tool(name = "getRoomCourses", description = "특정 강의실의 현재 학기 수업 일정을 조회합니다. 개인 시간표나 사용자 수강정보에는 사용하지 않습니다.")
    public GetRoomCoursesToolResult getRoomCourses(
            @ToolParam(description = "강의실 PLACE ID와 선택적 요일") GetRoomCoursesToolRequest request) {
        return adapter.getRoomCourses(request);
    }

    @Tool(name = "getCampusStatus", description = "현재 학기, 방학 여부, 고연전 기간 여부를 조회합니다. 건물 운영시간이나 과거·미래 일정 조회에는 사용하지 않습니다.")
    public CampusStatusToolResult getCampusStatus() {
        return adapter.getCampusStatus();
    }
}
