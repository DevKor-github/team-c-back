package devkor.com.teamcback.domain.navigate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
public class GetRouteRes {
    @Schema(description = "소요 시간", example = "100")
    private Long duration;
    @Schema(description = "상세 경로")
    private List<PartialRouteRes> path;
    @Schema(description = "경로 관련 오류사항")
    private String description;

    public GetRouteRes(Long duration, List<PartialRouteRes> path){
        this.duration = duration;
        this.path = path;
    }

    public GetRouteRes(String description){
        this.description = description;
    }

    public void addPath(PartialRouteRes path){
        this.path.add(path);
    }

}
