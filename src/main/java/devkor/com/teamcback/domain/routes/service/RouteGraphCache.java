package devkor.com.teamcback.domain.routes.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Edge;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.global.exception.exception.AdminException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_NODE_DATA;

/**
 * 경로 탐색 그래프(노드·엣지)를 건물 단위로 인메모리 캐싱.
 * 동일한 그래프를 매 요청마다 DB에서 재구성하지 않아 OOM을 방지한다.
 */
@Component
@RequiredArgsConstructor
public class RouteGraphCache {

    private static final String SEPARATOR = ",";
    private static final double INDOOR_ROUTE_WEIGHT = 0.3;
    private static final long OUTDOOR_ID = 0L;

    private final NodeRepository nodeRepository;

    /** 캐시 키: "buildingId:sortedNodeTypes" → 라우팅 노드 목록 */
    private final Cache<String, List<Node>> nodeCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();

    /** 캐시 키: "buildingId:sortedNodeTypes:isInnerRoute" → 노드별 엣지 맵 */
    private final Cache<String, Map<Long, List<Edge>>> edgeCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();

    /**
     * 건물 + 조건에 해당하는 라우팅 노드 목록 반환 (캐시 우선).
     */
    @Transactional(readOnly = true)
    public List<Node> getNodes(Building building, List<NodeType> nodeTypes) {
        String key = building.getId() + ":" + buildNodeTypeKey(nodeTypes);
        return nodeCache.get(key, k ->
            nodeRepository.findByBuildingAndRoutingAndTypeIn(building, true, nodeTypes)
        );
    }

    /**
     * 건물 + 조건에 해당하는 엣지 맵 반환 (캐시 우선).
     * 값은 read-only이므로 여러 요청이 동일 Map 인스턴스를 안전하게 공유 가능.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<Edge>> getEdges(Building building, List<NodeType> nodeTypes, boolean isInnerRoute) {
        String key = building.getId() + ":" + buildNodeTypeKey(nodeTypes) + ":" + isInnerRoute;
        return edgeCache.get(key, k -> buildEdgeMap(building, nodeTypes, isInnerRoute));
    }

    /**
     * 노드 데이터 변경 시 캐시를 전체 무효화.
     * (어드민 노드 수정 API 호출 시 연동 가능)
     */
    public void evictAll() {
        nodeCache.invalidateAll();
        edgeCache.invalidateAll();
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private Map<Long, List<Edge>> buildEdgeMap(Building building, List<NodeType> nodeTypes, boolean isInnerRoute) {
        List<Node> nodes = getNodes(building, nodeTypes);
        boolean isOutdoor = building.getId() == OUTDOOR_ID;
        Map<Long, List<Edge>> edgeMap = new HashMap<>();

        for (Node node : nodes) {
            String rawAdjacentNode = node.getAdjacentNode();
            String rawDistance = node.getDistance();
            if (rawAdjacentNode == null || rawDistance == null
                    || rawAdjacentNode.isEmpty() || rawDistance.isEmpty()) {
                continue;
            }

            Long[] nextNodeIds;
            Long[] distances;
            try {
                nextNodeIds = convertStringToArray(rawAdjacentNode);
                distances   = convertStringToArray(rawDistance);
            } catch (NumberFormatException e) {
                throw new AdminException(INCORRECT_NODE_DATA,
                    "노드" + node.getId() + "의 인접 노드 혹은 거리에 잘못된 입력이 있습니다.");
            }
            if (nextNodeIds.length != distances.length) {
                throw new AdminException(INCORRECT_NODE_DATA,
                    "노드" + node.getId() + "의 인접 노드와 거리 개수가 다릅니다.");
            }

            List<Edge> edges = new ArrayList<>();
            for (int i = 0; i < nextNodeIds.length; i++) {
                long weight = (isInnerRoute && !isOutdoor)
                    ? Math.round(distances[i] * INDOOR_ROUTE_WEIGHT)
                    : distances[i];
                edges.add(new Edge(distances[i], weight, node.getId(), nextNodeIds[i]));
            }
            edgeMap.put(node.getId(), edges);
        }
        return edgeMap;
    }

    private String buildNodeTypeKey(List<NodeType> nodeTypes) {
        return nodeTypes.stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));
    }

    private Long[] convertStringToArray(String str) {
        String[] parts = str.split(SEPARATOR);
        Long[] arr = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Long.parseLong(parts[i]);
        }
        return arr;
    }
}
