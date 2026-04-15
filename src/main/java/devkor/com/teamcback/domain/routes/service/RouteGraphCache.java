//package devkor.com.teamcback.domain.routes.service;
//
//import devkor.com.teamcback.domain.building.entity.Building;
//import devkor.com.teamcback.domain.building.entity.ConnectedBuilding;
//import devkor.com.teamcback.domain.building.repository.BuildingRepository;
//import devkor.com.teamcback.domain.building.repository.ConnectedBuildingRepository;
//import devkor.com.teamcback.domain.place.entity.Place;
//import devkor.com.teamcback.domain.place.entity.PlaceType;
//import devkor.com.teamcback.domain.place.repository.PlaceRepository;
//import devkor.com.teamcback.domain.routes.entity.Node;
//import devkor.com.teamcback.domain.routes.repository.NodeRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.stream.Collectors;
//
///**
// * 길찾기 그래프 인메모리 캐시.
// * 서버 기동 시 DB에서 전체 노드/엣지/건물 데이터를 로드하여 메모리에 유지.
// * 어드민 노드 CRUD 시 해당 건물 그래프만 부분 갱신.
// * 건물 운영여부(isOperating) 변경 시 ConcurrentHashMap 단일 항목만 갱신.
// *
// * 메모리 규모 (17,500 노드 / 50 건물 기준):
// *   - 노드 객체: ~5.3 MB
// *   - 엣지 raw 데이터(long[][]): ~1.4 MB
// *   - 인덱스 맵 / 빌딩 / 연결빌딩: ~1 MB
// *   합계 약 8~12 MB
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RouteGraphCache {
//
//    private final NodeRepository nodeRepository;
//    private final BuildingRepository buildingRepository;
//    private final ConnectedBuildingRepository connectedBuildingRepository;
//    private final PlaceRepository placeRepository;
//
//    private static final String SEPARATOR = ",";
//
//    /**
//     * 불변 스냅샷.
//     * 어드민 노드 변경 시 해당 건물 데이터만 교체한 새 스냅샷으로 원자적(atomic) 교체.
//     *
//     * @param nodeById            nodeId → Node (경로탐색용 전체 노드)
//     * @param nodesByBuilding     buildingId → 해당 건물의 routing=true 노드 목록
//     * @param rawEdgesByNode      nodeId → [[인접nodeId, 거리], ...] (String 파싱 비용 제거)
//     * @param buildingById        buildingId → Building
//     * @param allBuildings        전체 건물 목록 (node 필드 eagerly loaded — addInBetweenBuildings 용)
//     * @param connectedBuildingIds buildingId → 연결된 건물 ID Set
//     * @param shuttleBusStops     셔틀버스 정류장 Place 목록
//     */
//    public record GraphSnapshot(
//        Map<Long, Node> nodeById,
//        Map<Long, List<Node>> nodesByBuilding,
//        Map<Long, long[][]> rawEdgesByNode,
//        Map<Long, Building> buildingById,
//        List<Building> allBuildings,
//        Map<Long, Set<Long>> connectedBuildingIds,
//        List<Place> shuttleBusStops
//    ) {}
//
//    // 메인 그래프 스냅샷 — lock-free 읽기 / 희귀한 쓰기
//    private final AtomicReference<GraphSnapshot> snapshotRef = new AtomicReference<>();
//
//    // 건물 운영여부 — 스케줄러가 분 단위로 갱신하므로 ConcurrentHashMap으로 별도 관리
//    private final ConcurrentHashMap<Long, Boolean> buildingOperating = new ConcurrentHashMap<>();
//
//    // ──────────────────────────────────────────────────────────────
//    // 초기화
//    // ──────────────────────────────────────────────────────────────
//
//    /**
//     * ApplicationReadyEvent 시점에 초기화.
//     * @Transactional을 통해 Hibernate 세션 내에서 lazy 관계(building.node)를 안전하게 로드.
//     */
//    @EventListener(ApplicationReadyEvent.class)
//    @Transactional(readOnly = true)
//    public void init() {
//        log.info("[RouteGraphCache] 초기화 시작");
//        refreshAll();
//        GraphSnapshot snap = snapshotRef.get();
//        log.info("[RouteGraphCache] 초기화 완료 — 노드 {}개, 건물 {}개",
//            snap.nodeById().size(), snap.buildingById().size());
//    }
//
//    // ──────────────────────────────────────────────────────────────
//    // 조회 API
//    // ──────────────────────────────────────────────────────────────
//
//    public GraphSnapshot getSnapshot() {
//        return snapshotRef.get();
//    }
//
//    public boolean isBuildingOperating(Long buildingId) {
//        return buildingOperating.getOrDefault(buildingId, true);
//    }
//
//    // ──────────────────────────────────────────────────────────────
//    // 갱신 API
//    // ──────────────────────────────────────────────────────────────
//
//    /**
//     * 건물 운영여부 변경 (스케줄러에서 호출).
//     * ConcurrentHashMap 단일 put — 매우 빠름.
//     */
//    public void setBuildingOperating(Long buildingId, boolean operating) {
//        buildingOperating.put(buildingId, operating);
//    }
//
//    /**
//     * 특정 건물의 노드/엣지 캐시 갱신 (어드민 노드 CRUD 후 호출).
//     * 해당 건물 데이터만 교체한 새 스냅샷을 원자적으로 swap.
//     * @Transactional — 호출자(AdminRouteService)의 트랜잭션에 참여하여 최신 DB 상태를 읽음.
//     */
//    @Transactional(readOnly = true)
//    public synchronized void refreshBuildingGraph(Long buildingId) {
//        GraphSnapshot old = snapshotRef.get();
//        if (old == null) return;
//
//        Building building = buildingRepository.findById(buildingId).orElse(null);
//        if (building == null) return;
//
//        List<Node> newNodes = nodeRepository.findByBuildingAndRouting(building, true);
//
//        Map<Long, Node> newNodeById = new HashMap<>(old.nodeById());
//        Map<Long, List<Node>> newNodesByBuilding = new HashMap<>(old.nodesByBuilding());
//        Map<Long, long[][]> newRawEdgesByNode = new HashMap<>(old.rawEdgesByNode());
//        Map<Long, Building> newBuildingById = new HashMap<>(old.buildingById());
//
//        // 기존 건물 노드/엣지 제거
//        List<Node> oldNodes = old.nodesByBuilding().getOrDefault(buildingId, Collections.emptyList());
//        for (Node oldNode : oldNodes) {
//            newNodeById.remove(oldNode.getId());
//            newRawEdgesByNode.remove(oldNode.getId());
//        }
//
//        // 새 노드/엣지 추가
//        for (Node node : newNodes) {
//            newNodeById.put(node.getId(), node);
//            long[][] rawEdges = parseRawEdges(node);
//            if (rawEdges != null) {
//                newRawEdgesByNode.put(node.getId(), rawEdges);
//            }
//        }
//        newNodesByBuilding.put(buildingId, Collections.unmodifiableList(newNodes));
//        newBuildingById.put(buildingId, building);
//        buildingOperating.put(buildingId, building.isOperating());
//
//        snapshotRef.set(new GraphSnapshot(
//            Collections.unmodifiableMap(newNodeById),
//            Collections.unmodifiableMap(newNodesByBuilding),
//            Collections.unmodifiableMap(newRawEdgesByNode),
//            Collections.unmodifiableMap(newBuildingById),
//            old.allBuildings(),           // 건물 목록 구조 변경 없음 (CRUD는 노드 단위)
//            old.connectedBuildingIds(),
//            old.shuttleBusStops()
//        ));
//        log.info("[RouteGraphCache] 건물 {} 그래프 갱신 완료 (노드 {}개)", buildingId, newNodes.size());
//    }
//
//    /**
//     * 전체 캐시 재빌드 (기동 시 또는 광범위한 데이터 변경 시).
//     * synchronized — 동시 재빌드 방지.
//     */
//    @Transactional(readOnly = true)
//    public synchronized void refreshAll() {
//        // 1. 건물 (node 필드 eager fetch — addInBetweenBuildings에서 building.getNode() 사용)
//        List<Building> allBuildings = buildingRepository.findAllWithNode();
//        Map<Long, Building> buildingById = new HashMap<>(allBuildings.size() * 2);
//        for (Building b : allBuildings) {
//            buildingById.put(b.getId(), b);
//            buildingOperating.put(b.getId(), b.isOperating());
//        }
//
//        // 2. 전체 routing 노드 + 엣지 파싱
//        Map<Long, Node> nodeById = new HashMap<>(25000);
//        Map<Long, List<Node>> nodesByBuilding = new HashMap<>(allBuildings.size() * 2);
//        Map<Long, long[][]> rawEdgesByNode = new HashMap<>(25000);
//
//        for (Building building : allBuildings) {
//            List<Node> nodes = nodeRepository.findByBuildingAndRouting(building, true);
//            nodesByBuilding.put(building.getId(), Collections.unmodifiableList(nodes));
//            for (Node node : nodes) {
//                nodeById.put(node.getId(), node);
//                long[][] rawEdges = parseRawEdges(node);
//                if (rawEdges != null) {
//                    rawEdgesByNode.put(node.getId(), rawEdges);
//                }
//            }
//        }
//
//        // 3. 연결 건물 맵
//        Map<Long, Set<Long>> connectedBuildingIds = new HashMap<>();
//        for (ConnectedBuilding cb : connectedBuildingRepository.findAll()) {
//            connectedBuildingIds
//                .computeIfAbsent(cb.getBuilding().getId(), k -> new HashSet<>())
//                .add(cb.getConnectedBuildingId());
//        }
//        // unmodifiable inner sets
//        connectedBuildingIds.replaceAll((k, v) -> Collections.unmodifiableSet(v));
//
//        // 4. 셔틀버스 정류장
//        List<Place> shuttleBusStops = placeRepository.findAllByType(PlaceType.SHUTTLE_BUS);
//
//        snapshotRef.set(new GraphSnapshot(
//            Collections.unmodifiableMap(nodeById),
//            Collections.unmodifiableMap(nodesByBuilding),
//            Collections.unmodifiableMap(rawEdgesByNode),
//            Collections.unmodifiableMap(buildingById),
//            Collections.unmodifiableList(allBuildings),
//            Collections.unmodifiableMap(connectedBuildingIds),
//            Collections.unmodifiableList(shuttleBusStops)
//        ));
//    }
//
//    // ──────────────────────────────────────────────────────────────
//    // 내부 유틸
//    // ──────────────────────────────────────────────────────────────
//
//    /**
//     * 노드의 adjacentNode/distance String을 long[N][2] 배열로 파싱.
//     * index 0: 인접 nodeId, index 1: 거리
//     * 파싱 실패 또는 데이터 없으면 null 반환.
//     */
//    private long[][] parseRawEdges(Node node) {
//        String rawAdjacentNode = node.getAdjacentNode();
//        String rawDistance = node.getDistance();
//
//        if (rawAdjacentNode == null || rawAdjacentNode.isEmpty()
//            || rawDistance == null || rawDistance.isEmpty()) {
//            return null;
//        }
//
//        try {
//            String[] nodeIds = rawAdjacentNode.split(SEPARATOR);
//            String[] distances = rawDistance.split(SEPARATOR);
//            if (nodeIds.length != distances.length) {
//                log.warn("[RouteGraphCache] 노드 {} adjacentNode/distance 개수 불일치 — 스킵", node.getId());
//                return null;
//            }
//            long[][] result = new long[nodeIds.length][2];
//            for (int i = 0; i < nodeIds.length; i++) {
//                result[i][0] = Long.parseLong(nodeIds[i].trim());
//                result[i][1] = Long.parseLong(distances[i].trim());
//            }
//            return result;
//        } catch (NumberFormatException e) {
//            log.warn("[RouteGraphCache] 노드 {} 엣지 파싱 실패: {}", node.getId(), e.getMessage());
//            return null;
//        }
//    }
//}
