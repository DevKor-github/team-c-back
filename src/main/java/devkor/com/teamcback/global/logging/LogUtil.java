package devkor.com.teamcback.global.logging;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
public class LogUtil {
    public void logClick(String buildingName, String placeName, Double floor, String categoryName) {
        log.info("click_event",
            kv("buildingName", buildingName),
            kv("placeName", placeName),
            kv("floor", floor),
            kv("categoryName", categoryName)
        );
    }

    public void logSearch(String keyword) {
        log.info("search_event", kv("keyword", keyword));
    }

    public void logRoute(Building startBuilding, Place startPlace, Building endBuilding, Place endPlace, List<Conditions> conditions) {
        log.info("route_event",
            kv("startBuildingName", (startBuilding != null) ? startBuilding.getName() : startPlace.getBuilding().getName()),
            kv("endBuildingName", (endBuilding != null) ? endBuilding.getName() : endPlace.getBuilding().getName()),
            kv("startPlaceName", (startPlace != null) ? startPlace.getName() : null),
            kv("endPlaceName", (endPlace != null) ? endPlace.getName() : null),
            kv("conditions", (conditions != null) ? conditions.stream().map(Enum::name).toList() : null)
        );
    }
}
