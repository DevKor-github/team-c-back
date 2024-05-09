package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class GetSearchLogRes {
    private Long id;
    private String name;
    private PlaceType type;
    private String searchedAt;

    public GetSearchLogRes(SearchLog searchLog) {
        this.id = searchLog.getId();
        this.name = searchLog.getName();
        this.type = searchLog.getType();
        this.searchedAt = searchLog.getSearchedAt();
    }
}
