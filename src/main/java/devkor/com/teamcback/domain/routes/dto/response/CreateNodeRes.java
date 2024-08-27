package devkor.com.teamcback.domain.routes.dto.response;

import devkor.com.teamcback.domain.routes.entity.Node;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "노드 생성 응답 dto")
@Getter
public class CreateNodeRes {
    private Long nodeId;

    public CreateNodeRes(Node node) {
        nodeId = node.getId();
    }
}
