package devkor.com.teamcback.domain.routes.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.building.repository.ConnectedBuildingRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.dto.response.DijkstraRes;
import devkor.com.teamcback.domain.routes.dto.response.GetGraphRes;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.routes.entity.*;
import devkor.com.teamcback.domain.routes.repository.CheckpointRepository;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static devkor.com.teamcback.domain.routes.entity.Conditions.BARRIERFREE;
import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final NodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final CheckpointRepository checkpointRepository;
    private final ConnectedBuildingRepository connectedBuildingRepository;
    private static final long OUTDOOR_ID = 0L;
    private static final String SEPARATOR = ",";
    private static final Long INF = Long.MAX_VALUE;
    private static final Double INIT_OUTDOOR_DISTANCE = Double.MAX_VALUE;
    private static final Double MAX_OUTDOOR_DISTANCE = 0.003;
    private static final Double MIN_OUTDOOR_DISTANCE = 0.0001;
    private static final Double INDOOR_ROUTE_WEIGHT = 0.3;

    /**
     * 메인 경로탐색 메서드
     */
    @Transactional(readOnly = true)
    public List<GetRouteRes> findRoute(LocationType startType, Long startId, Double startLat, Double startLong,
                                       LocationType endType, Long endId, Double endLat, Double endLong, List<Conditions> conditions){

        if (conditions == null){
            conditions = new ArrayList<>();
        }
        // 출발, 도착 노드 검색
        Node startNode = getNodeByType(startType, startId, startLat, startLong);
        Node endNode = getNodeByType(endType, endId, endLat, endLong);

        // 에러 조건 확인
        checkLocationError(startNode, endNode, startType, endType, startId, endId);

        // 길찾기 응답 리스트 생성
        List<GetRouteRes> routeRes = new ArrayList<>();

        // 연결된 건물 찾기
        List<Building> buildingList = getBuildingsForRoute(startNode, endNode);

        // 경로를 하나만 반환하는 경우
        GetGraphRes graphRes = getGraph(buildingList, startNode, endNode, conditions);
        DijkstraRes route = dijkstra(graphRes, startNode, endNode);

        // 경로를 하나만 반환한다면 경로가 없을 때 예외 처리
        if (route.getPath().isEmpty()) {
            throw new GlobalException(NOT_FOUND_ROUTE);
        }
        routeRes.add(buildRouteResponse(route, startType == LocationType.BUILDING, endType == LocationType.BUILDING));

        // 베리어프리만 추가로 적용하는 경우(임시)
//        GetGraphRes graphRes2 = getGraph(buildingList, startNode, endNode, List.of(BARRIERFREE));
//        DijkstraRes route2 = dijkstra(graphRes2.getGraphNode(), graphRes2.getGraphEdge(), startNode, endNode);
//        if(route.getPath().isEmpty() && route2.getPath.isEmpty()) throw new GlobalException(NOT_FOUND_ROUTE);
//        routeRes.add(buildRouteResponse(route2, isStartBuilding, isEndBuilding));

        return routeRes;
    }

    /**
     * 에러 조건 확인
     */
    private void checkLocationError(Node startNode, Node endNode, LocationType startType, LocationType endType, Long startId, Long endId) {
        boolean isStartBuilding = startType == LocationType.BUILDING;
        boolean isEndBuilding = endType == LocationType.BUILDING;

        // 1. 출발 건물, 도착 건물이 같은 경우
        if (isStartBuilding && isEndBuilding) {
            if (startId.equals(endId)) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        // 2. 같은 건물 내 장소 간의 경로 요청의 경우
        if (isStartBuilding && endType == LocationType.PLACE) {
            if (startId.equals(endNode.getBuilding().getId())) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        if (isEndBuilding && startType == LocationType.PLACE) {
            if (endId.equals(startNode.getBuilding().getId())) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        // 3. 야외에서 너무 가까운 경우
        if (startNode.getBuilding().getId().equals(OUTDOOR_ID) && endNode.getBuilding().getId().equals(OUTDOOR_ID)){
            if (getEuclidDistance(startNode.getLatitude(), startNode.getLongitude(), endNode.getLatitude(), endNode.getLongitude()) < MIN_OUTDOOR_DISTANCE) {
                throw new GlobalException(COORDINATES_TOO_NEAR);
            }
        }
    }


    /**
     * 타입에 맞는 Node 찾기
     */
    private Node getNodeByType(LocationType type, Long id, Double latitude, Double longitude) {
        return switch (type) {
            case COORD -> findNearestNode(latitude, longitude);
            case BUILDING -> findBuilding(id).getNode();
            case PLACE -> findPlace(id).getNode();
            case NODE -> findNode(id);
        };
    }

    /**
     * 주어진 좌표값에서 가장 가까운 외부 노드 찾기
     */
    private Node findNearestNode(Double latitude, Double longitude){
        List<Node> graphNode = new ArrayList<>(findAllNode(findBuilding(OUTDOOR_ID)));
        double shortestDistance = INIT_OUTDOOR_DISTANCE;
        Node nearestNode = null;

        for (Node node: graphNode){
            double thisDistance = getEuclidDistance(latitude, longitude, node.getLatitude(), node.getLongitude());
            if (nearestNode == null || shortestDistance > thisDistance){
                nearestNode = node;
                shortestDistance = thisDistance;
            }
        }
        if(nearestNode == null) {
            throw new GlobalException(NOT_FOUND_NODE);
        }
        if (getEuclidDistance(latitude, longitude, nearestNode.getLatitude(), nearestNode.getLongitude()) > MAX_OUTDOOR_DISTANCE){
            throw new GlobalException(COORDINATES_TOO_FAR);
        }
        return nearestNode;
    }

    private double getEuclidDistance(double startX, double startY, double endX, double endY) {
        return Math.sqrt(Math.pow((startX - endX), 2) + Math.pow((startY - endY), 2));
    }

    /**
     * 탐색 알고리즘의 효율성을 위해 이동할 만한 건물들만 추리는 메서드
     */
    private List<Building> getBuildingsForRoute(Node startNode, Node endNode) {
        List<Building> buildingList = new ArrayList<>();
        buildingList.add(findBuilding(OUTDOOR_ID)); // 외부 경로 추가
        if(!buildingList.contains(startNode.getBuilding())) buildingList.add(startNode.getBuilding());
        if(!buildingList.contains(endNode.getBuilding())) buildingList.add(endNode.getBuilding());

        addConnectedBuildings(startNode.getBuilding(), buildingList);
        addConnectedBuildings(endNode.getBuilding(), buildingList);

        return buildingList;
    }

    /**
     * 출발/도착지에 연결된 건물들이 있는 경우 buildingList에 추가하는 메서드
     * 연쇄적으로 연결된 건물들도 반영하도록(ex: 엘포관-백기-중지-SK미래관...) 수정
     */
    private void addConnectedBuildings(Building startBuilding, List<Building> buildingList) {
        Queue<Building> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();

        queue.add(startBuilding);
        visited.add(startBuilding.getId());

        while (!queue.isEmpty()) {
            Building currentBuilding = queue.poll();

            List<Long> connectedBuildingIds = connectedBuildingRepository.findConnectedBuildingsByBuilding(currentBuilding);
            for (Long connectedBuildingId : connectedBuildingIds) {
                if (!visited.contains(connectedBuildingId)) {
                    Building connectedBuilding = findBuilding(connectedBuildingId);
                    buildingList.add(connectedBuilding);
                    queue.add(connectedBuilding);
                    visited.add(connectedBuildingId);
                }
            }
        }
    }


    /**
     * 그래프 요소 찾기(node, edge 묶음)
     * 노드 테이블의 String 인접 노드와 거리를 그래프로 변환
     */
    private GetGraphRes getGraph(List<Building> buildingList, Node startNode, Node endNode, List<Conditions> conditions){
        List<Node> graphNode = new ArrayList<>();
        Map<Long, List<Edge>> graphEdge = new HashMap<>();

        // 조건에 따른 노드 검색

        for (Building building : buildingList){
            graphNode.addAll(findNodeWithConditions(building, conditions));
        }

        if (!graphNode.contains(startNode)){
            graphNode.add(startNode);
        }
        if (!graphNode.contains(endNode)){
            graphNode.add(endNode);
        }

        for (Node node: graphNode){
            String rawAdjacentNode = node.getAdjacentNode();
            String rawDistance = node.getDistance();

            if (rawAdjacentNode == null || rawDistance == null || rawAdjacentNode.isEmpty() || rawDistance.isEmpty()) continue;

            Long[] nextNodeId;
            Long[] distance;
            try {
                nextNodeId = convertStringToArray(node.getAdjacentNode());
                distance = convertStringToArray(node.getDistance());
            } catch (NumberFormatException e) {
                throw new AdminException(INCORRECT_NODE_DATA, "노드" + node.getId() + "의 인접 노드 혹은 거리에 잘못된 입력이 있습니다.");
            }

            // 인접 노드와 거리의 개수가 맞지 않을 때
            if (nextNodeId.length != distance.length) {
                throw new AdminException(INCORRECT_NODE_DATA, "노드" + node.getId() + "의 인접 노드와 거리 개수가 다릅니다.");
            }

            if(!graphEdge.containsKey(node.getId())) {
                graphEdge.put(node.getId(), new ArrayList<>());
            }
            for (int i = 0; i < nextNodeId.length; i++) {
                long weight = (conditions.contains(Conditions.INNERROUTE) && node.getBuilding().getId() != OUTDOOR_ID) ? Math.round(distance[i] * INDOOR_ROUTE_WEIGHT) : distance[i];
                Edge edge = new Edge(distance[i], weight, node.getId(), nextNodeId[i]);
                graphEdge.get(node.getId()).add(edge);
            }
        }
        return new GetGraphRes(graphNode, graphEdge);
    }

    /**
     * 조건에 맞는 노드만 포함
     * 추후 이 메서드와 getGraph 수정하여 operating 여부, 실내 탐색 필요.
     */
    private List<Node> findNodeWithConditions(Building building, List<Conditions> conditions){
        List<NodeType> nodeTypes = new ArrayList<>(Arrays.asList(NodeType.NORMAL, NodeType.STAIR, NodeType.ELEVATOR, NodeType.ENTRANCE, NodeType.CHECKPOINT));
        if (conditions != null){
            if (conditions.contains(BARRIERFREE)){
                nodeTypes.remove(NodeType.STAIR);
            }
            else if (conditions.contains(Conditions.SHUTTLE)){
                nodeTypes.add(NodeType.SHUTTLE);
            }
        }
        return nodeRepository.findByBuildingAndRoutingAndTypeIn(building, true, nodeTypes);
    }

    /**
     * 다익스트라 경로 생성
     */
    private DijkstraRes dijkstra(GetGraphRes graphRes, Node startNode, Node endNode) {
        List<Node> nodes = graphRes.getGraphNode();
        Map<Long, List<Edge>> edges = graphRes.getGraphEdge();
        Map<Long, Long> distances = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> priorityQueue = new PriorityQueue<>();
        Set<Long> visitedNodes = new HashSet<>();

        // 모든 노드 초기화
        for (Node node : nodes) {
            if (node.equals(startNode)) {
                distances.put(node.getId(), 0L);
                priorityQueue.add(new NodeDistancePair(node.getId(), 0L));
            } else {
                distances.put(node.getId(), INF);
            }
            previousNodes.put(node.getId(), null);
        }

        // Dijkstra 실행 (weight 기준)
        while (!priorityQueue.isEmpty()) {
            NodeDistancePair currentPair = priorityQueue.poll();
            Long currentNode = currentPair.node;

            if (visitedNodes.contains(currentNode)) continue;
            visitedNodes.add(currentNode);

            if (currentNode.equals(endNode.getId())) break;

            if (!edges.containsKey(currentNode)) continue;
            for (Edge edge : edges.get(currentNode)) {
                Long neighbor = edge.getEndNode();
                Long currentDistance = distances.get(currentNode);
                if (currentDistance == null) continue;

                Long newDist = currentDistance + edge.getWeight(); //weight 기반 탐색으로 수정
                Long neighborDist = distances.get(neighbor);
                if (neighborDist == null || newDist < neighborDist) {
                    distances.put(neighbor, newDist);
                    previousNodes.put(neighbor, currentNode);
                    priorityQueue.add(new NodeDistancePair(neighbor, newDist));
                }
            }
        }

        //path 생성
        List<Node> path = new ArrayList<>();
        Node pathPrevNode = null;
        Long finalDistance = 0L;
        for (Long at = endNode.getId(); at != null; at = previousNodes.get(at)) {
            Node node = nodeRepository.findById(at).orElseThrow(() -> new GlobalException(NOT_FOUND_ROUTE));
            if (pathPrevNode != null) {
                Edge edge = findEdge(edges, node.getId(), pathPrevNode.getId());
                finalDistance += edge.getDistance();
            }
            path.add(node);
            pathPrevNode = node;
        }
        Collections.reverse(path);

        //예외처리: path가 제대로 나오지 않는 경우. 즉, 경로가 존재하지 않는 경우
        if (path.isEmpty() || !path.get(0).equals(startNode)) {
            throw new GlobalException(NOT_FOUND_ROUTE);
        }

        return new DijkstraRes(finalDistance, path);
    }
    private Edge findEdge(Map<Long, List<Edge>> edges, Long from, Long to) {
        return edges.get(from).stream()
                .filter(edge -> edge.getEndNode().equals(to))
                .findFirst()
                .orElse(null);
    }

    /**
     * 경로를 분할하고 응답 형식에 맞게 변환
     */
    private GetRouteRes buildRouteResponse(DijkstraRes route, boolean isStartBuilding, boolean isEndBuilding) {
//        if (route.getPath().isEmpty()) return new GetRouteRes(1);//경로 미탐색 막기 위해 임의로 추가

        Long duration = route.getDistance();
        List<List<Node>> path = cutRoute(route.getPath()); // 분할된 경로

        //시작, 끝이 건물인 경우 해당 노드 지우기
        //시작, 끝이 건물인 경우 해당 노드 지우기
        if (isStartBuilding) {
            // 첫번째 path의 길이에 따라 삭제 다르게 하기
            if(path.get(0).size() > 1) {
                path.get(0).remove(0);
            } else {
                path.remove(0);
            }
        }
        if (isEndBuilding) path.get(path.size()-1).remove(path.get(path.size()-1).size()-1);

        List<PartialRouteRes> totalRoute = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            List<Node> thisPath = path.get(i);
            boolean isOutside = thisPath.get(0).getBuilding().getId() == OUTDOOR_ID;

            // 노드의 좌표를 리스트 형태로 변환
            List<List<Double>> partialRoute = convertNodesToCoordinates(thisPath, isOutside);

            PartialRouteRes partialRouteRes = isOutside
                    ? new PartialRouteRes(partialRoute) // 야외 경로
                    : new PartialRouteRes(thisPath.get(0).getBuilding().getId(), thisPath.get(0).getFloor(), partialRoute); // 실내 경로

            // 부분 경로의 마지막 노드인 경우 설명 추가
            if (i + 1 == path.size()) {
                partialRouteRes.setInfo(makeInfo(thisPath.get(thisPath.size() - 1), null));
            } else {
                partialRouteRes.setInfo(makeInfo(thisPath.get(thisPath.size() - 1), path.get(i + 1).get(0)));
            }

            totalRoute.add(partialRouteRes);
        }

        return new GetRouteRes(duration, totalRoute);
    }

    /**
     * 전체 경로 리스트를 받아 층별, 건물별로 끊기
     */
    private List<List<Node>> cutRoute(List<Node> route) {
//        if (route.isEmpty()) throw new Error("파싱할 리스트가 비었습니다.");

        List<List<Node>> returnRoute = new ArrayList<>();
        List<Node> partialRoute = new ArrayList<>();
        int count = 0;
        Node thisNode, nextNode;


        while (count < route.size() - 1) {
            thisNode = route.get(count);
            nextNode = route.get(count + 1);

            // 새로운 건물로 이동할 때 & 체크포인트일때 & 외부에서 새로운 건물로 들어갈 때(입구 분리) 경로분할
            if ((!thisNode.getBuilding().equals(nextNode.getBuilding()) && thisNode.getBuilding().getId() != OUTDOOR_ID)
                    || (thisNode.getType() != nextNode.getType() && thisNode.getType() == NodeType.CHECKPOINT)
                    || (thisNode.getBuilding().getId() == OUTDOOR_ID && nextNode.getType() == NodeType.ENTRANCE)) {
                partialRoute.add(thisNode);
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();
            }

            // 같은 건물 내에서 층 이동 시 경로 분할, 중간 층 생략
            else if (!Objects.equals(thisNode.getFloor(), nextNode.getFloor())) {
                partialRoute.add(thisNode);

                // 계단/엘리베이터를 통한 연속적인 층 이동을 감지하여 중간 층을 생략
                while (count < route.size() - 1 && !Objects.equals(thisNode.getFloor(), nextNode.getFloor())) {
                    thisNode = route.get(count);
                    nextNode = route.get(count + 1);
                    count++;
                }
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();

                // 끝 층의 시작 노드를 새 경로로 추가
                partialRoute.add(thisNode);
            }
            else {
                partialRoute.add(thisNode);
            }

            count++;
        }

        // 마지막 노드를 추가
        if (count < route.size()) {
            partialRoute.add(route.get(count));
        }
        if (!partialRoute.isEmpty()) {
            returnRoute.add(new ArrayList<>(partialRoute));
        }

        return returnRoute;
    }

    /**
     * 좌표 정보를 제공하는 형식에 맞게 x,y 또는 lat,long 좌표로 바꿔 주는 메서드
     */
    private List<List<Double>> convertNodesToCoordinates(List<Node> nodes, boolean isOutside) {
        List<List<Double>> coordinates = new ArrayList<>();
        for (Node node : nodes) {
            if (isOutside) {
                coordinates.add(Arrays.asList(node.getLatitude(), node.getLongitude(), node.getId().doubleValue()));
            } else {
                coordinates.add(Arrays.asList(node.getXCoord(), node.getYCoord(), node.getId().doubleValue()));
            }
        }
        return coordinates;
    }

    /**
     * 나눠진 기준으로 두 노드를 받아 비교하여 간단한 설명 생성
     * nextNode에 null이 들어오면 경로 안내가 끝난 상황이라고 판단
     */
    private String makeInfo(Node prevNode, Node nextNode){
        if (nextNode == null) return "도착";

        if (prevNode.getType() == NodeType.CHECKPOINT){
            String checkpointName = findCheckpoint(prevNode).getName();
            return checkpointName + "(으)로 이동하세요.";
        }

        Building prevNodeBuilding = prevNode.getBuilding();
        Building nextNodeBuilding = nextNode.getBuilding();

        // 건물 외부에서 내부로 들어가는 경우: prevNodeBuilding과 nextNodeBuilding이 같음(id=0L)
        // 미리 출입구에서 한 번 추가적으로 끊기 때문.
        if (Objects.equals(prevNodeBuilding, nextNodeBuilding) && prevNodeBuilding.getId() == OUTDOOR_ID){
            Building enteringBuilding = findLinkedBuilding(nextNode);
            return enteringBuilding.getName() + "(으)로 이동하세요.";
        }

        int nextNodeFloor = nextNode.getFloor().intValue();
        String floor = nextNodeFloor >= 0 ? Integer.toString(nextNodeFloor) : "B" + Math.abs(nextNodeFloor);

        // 그 외 건물이 같은 경우는 층 이동의 경우밖에 없음
        if (Objects.equals(prevNodeBuilding, nextNodeBuilding)){
            return floor + "층으로 이동하세요.";
        }

        // 끊긴 출입구 기준으로 바깥에서 안으로 들어가는 경우
        if (prevNodeBuilding.getId() == OUTDOOR_ID){
            return nextNodeBuilding.getName() + " " + floor + "층 출입구로 들어가세요.";
        }
        // 안에서 바깥으로 나가는 경우
        if (nextNodeBuilding.getId() == OUTDOOR_ID) return "출입구를 통해 밖으로 나가세요";

        // 건물 사이를 바로 이동하는 경우
        return "출입구를 통해 " + nextNodeBuilding.getName() + " " + floor + "층으로 이동하세요.";
    }

    /**
     * 노드에 연결된 건물을 찾는 메서드(들어오는 예상 노드는 ENTRANCE).
     * 건물 내부가 완료되지 않아 / 가장 가까운 출입구가 출입금지라 entrance와 연결된 내부 노드가 없을 수도 있는데, 이 경우 건물 노드로 찾는다
     */
    private Building findLinkedBuilding(Node node){
        Long[] adjacentNodeIds = convertStringToArray(node.getAdjacentNode());
        return buildingRepository.findByNodeIdIn(adjacentNodeIds)
                .orElseThrow(() -> new AdminException(INCORRECT_NODE_DATA,node.getId() + "번 노드에 연결된 건물이 없습니다"));
    }

    /**
     * String 형식의 adjacentNode와 distance를 배열로 변환
     */
    private Long[] convertStringToArray(String str) throws NumberFormatException {
        String[] strArr = str.split(SEPARATOR);
        Long[] arr = new Long[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            arr[i] = Long.parseLong(strArr[i]);
        }
        return arr;
    }

    // findEntity 메서드
    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private List<Node> findAllNode(Building building){
        return nodeRepository.findByBuildingAndRouting(building, true);
    }

    private Checkpoint findCheckpoint(Node node){
        return checkpointRepository.findByNode(node);
    }

    /**
     * 다익스트라용 클래스
     */
    private static class NodeDistancePair implements Comparable<NodeDistancePair> {
        Long node;
        Long distance;

        public NodeDistancePair(Long node, Long distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistancePair other) {
            return Long.compare(this.distance, other.distance);
        }
    }

}