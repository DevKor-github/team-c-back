package devkor.com.teamcback.domain.search.dto.request;

import devkor.com.teamcback.domain.search.entity.PlaceType;
import lombok.Getter;

@Getter
public class SaveSearchLogReq {
    private String name;
    private Long id;
    private PlaceType type;
}
