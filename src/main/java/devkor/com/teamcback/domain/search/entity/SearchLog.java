package devkor.com.teamcback.domain.search.entity;

import devkor.com.teamcback.domain.common.LocationType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchLog {
    private Long id;
    private String name;
    private LocationType type;
    private String searchedAt;

    public SearchLog(Long id, String name, LocationType type, String searchedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.searchedAt = searchedAt;
    }
}
