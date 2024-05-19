package devkor.com.teamcback.domain.navigate.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class OuterRouteRes {

    private String duration;
    private List<String> route;

    public OuterRouteRes(String duration, List<String> route){
        this.duration = duration;
        this.route = route;
    }
}
