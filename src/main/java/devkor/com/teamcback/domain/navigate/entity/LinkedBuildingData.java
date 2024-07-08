package devkor.com.teamcback.domain.navigate.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedBuildingData {
    private static final Map<Long, List<Long>> linkedBuildings = new HashMap<>();

    static {
        /*
        linkedBuildings.put(1L, List.of(2L, 3L)); // 건물 id 1에 연결된 건물들
        linkedBuildings.put(2L, List.of(1L, 3L)); // 건물 id 2에 연결된 건물들
        ...
         필요에 따라 위와 같은 식으로 다른 건물들의 연결 정보 추가
        */
    }

    public static List<Long> getLinkedBuildings(Long buildingId) {
        return linkedBuildings.getOrDefault(buildingId, List.of());
    }
}