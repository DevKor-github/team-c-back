package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.building.entity.BuildingEntrance;
import java.util.List;
import lombok.Getter;

@Getter
public class OuterRouteRes {

    private Long startEntranceId;
    private String startBuilding;
    private Long endEntranceId;
    private String endBuilding;
    private Integer duration;
    private List<Double[]> route;

    public OuterRouteRes(BuildingEntrance startEntrance, BuildingEntrance endEntrance,
        Integer duration, List<Double[]> route) {
        this.startEntranceId = startEntrance.getId();
        this.startBuilding = startEntrance.getBuilding().getName();
        this.endEntranceId = endEntrance.getId();
        this.endBuilding = endEntrance.getBuilding().getName();
        this.duration = duration;
        this.route = route;
    }
}
