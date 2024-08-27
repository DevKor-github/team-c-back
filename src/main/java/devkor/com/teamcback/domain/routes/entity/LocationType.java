package devkor.com.teamcback.domain.routes.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocationType {
    COORD, BUILDING, PLACE, NODE;

}
