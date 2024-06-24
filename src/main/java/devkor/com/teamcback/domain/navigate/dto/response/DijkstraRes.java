package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;
import lombok.Getter;

@Getter
public class DijkstraRes {
    private Long distance;
    private List<Node> path;

    public DijkstraRes(Long distance, List<Node> path){
        this.distance = distance;
        this.path = path;
    }
}
