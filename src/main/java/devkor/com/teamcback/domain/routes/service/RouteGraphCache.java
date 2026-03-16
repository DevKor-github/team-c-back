package devkor.com.teamcback.domain.routes.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 경로 탐색 그래프(노드)를 건물 단위로 인메모리 캐싱.
 * 엣지는 LazyEdgeMap을 통해 per-request 방식으로 지연 파싱해 Old gen 상주 메모리를 절감한다.
 */
@Component
@RequiredArgsConstructor
public class RouteGraphCache {

    private final NodeRepository nodeRepository;

    /** 캐시 키: "buildingId:sortedNodeTypes" → 라우팅 노드 목록 */
    private final Cache<String, List<Node>> nodeCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();

    /** 캐시 키: "buildingId:sortedNodeTypes" → nodeId → Node 맵 (경로 재구성 + 엣지 지연 파싱 소스) */
    private final Cache<String, Map<Long, Node>> nodeMapCache = Caffeine.newBuilder()
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
     * 건물 + 조건에 해당하는 nodeId→Node 맵 반환 (캐시 우선).
     * dijkstra 경로 재구성 및 LazyEdgeMap 엣지 파싱 소스로 사용.
     */
    @Transactional(readOnly = true)
    public Map<Long, Node> getNodeMap(Building building, List<NodeType> nodeTypes) {
        String key = building.getId() + ":" + buildNodeTypeKey(nodeTypes);
        return nodeMapCache.get(key, k -> {
            List<Node> nodes = getNodes(building, nodeTypes);
            Map<Long, Node> map = new HashMap<>(nodes.size(), 1.0f);
            for (Node node : nodes) map.put(node.getId(), node);
            return map;
        });
    }

    /**
     * 노드 데이터 변경 시 캐시를 전체 무효화.
     */
    public void evictAll() {
        nodeCache.invalidateAll();
        nodeMapCache.invalidateAll();
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private String buildNodeTypeKey(List<NodeType> nodeTypes) {
        return nodeTypes.stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));
    }
}
