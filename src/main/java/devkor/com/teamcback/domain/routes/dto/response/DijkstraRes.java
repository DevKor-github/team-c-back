package devkor.com.teamcback.domain.routes.dto.response;

import devkor.com.teamcback.domain.routes.entity.Node;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DijkstraRes {
    private Long distance;
    private List<Node> path;

    public DijkstraRes(Long distance, List<Node> path){
        this.distance = distance;
        this.path = path;
    }
}
