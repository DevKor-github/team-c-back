package devkor.com.teamcback.domain.search.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchLog extends BaseEntity {
    private String name;
    private Long id;
    private PlaceType type;
}
