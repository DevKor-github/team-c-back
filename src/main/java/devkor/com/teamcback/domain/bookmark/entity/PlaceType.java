package devkor.com.teamcback.domain.bookmark.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceType {
    BUILDING("building"),
    CLASSROOM("classroom"),
    FACILITY("facility");

    private final String name;
}
