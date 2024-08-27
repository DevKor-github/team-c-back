package devkor.com.teamcback.domain.routes.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BuildingIds {
    aegineung(1L, "애기능생활관"),
    woodang(2L, "우당교양관");

    private final Long buildingId;
    private final String name;

}
