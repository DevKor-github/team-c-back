package devkor.com.teamcback.global.logging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LogUtil {

    private static final Logger routeLog  = LoggerFactory.getLogger("ROUTE_LOG");
    private static final Logger searchLog = LoggerFactory.getLogger("SEARCH_LOG");
    private static final Logger clickLog  = LoggerFactory.getLogger("CLICK_LOG");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void logClick(String buildingName, String placeName, Double floor, String categoryName) {
        Map<String, Object> logMap = new HashMap<>();
        if(buildingName != null) logMap.put("buildingName", buildingName);
        if(placeName != null) logMap.put("placeName", placeName);
        if(floor != null) logMap.put("floor", floor);
        if(categoryName != null) logMap.put("categoryName", categoryName);

        try {
            clickLog.info(objectMapper.writeValueAsString(logMap));
        } catch (Exception ignored) { }
    }

    public void logSearch(String keyword) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("keyword", keyword);

        try {
            searchLog.info(objectMapper.writeValueAsString(logMap));
        } catch (Exception ignored) { }
    }

    public void logRoute(Building startBuilding, Place startPlace, Building endBuilding, Place endPlace, List<Conditions> conditions) {
        Map<String, Object> logMap = new HashMap<>();

        logMap.put("startBuildingName", (startBuilding != null) ? startBuilding.getName() : startPlace.getBuilding().getName());
        logMap.put("endBuildingName", (endBuilding != null) ? endBuilding.getName() : endPlace.getBuilding().getName());

        if (startPlace != null) logMap.put("startPlaceName", startPlace.getName());
        if (endPlace != null) logMap.put("endPlaceName", endPlace.getName());
        if (conditions != null) logMap.put("conditions", conditions.stream().map(Enum::name).toList());

        try {
            routeLog.info(objectMapper.writeValueAsString(logMap));
        } catch (Exception ignored) { }
    }
}
