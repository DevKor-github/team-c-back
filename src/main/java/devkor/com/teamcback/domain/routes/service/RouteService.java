package devkor.com.teamcback.domain.routes.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.building.repository.ConnectedBuildingRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.dto.response.DijkstraRes;
import devkor.com.teamcback.domain.routes.dto.response.GetGraphRes;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.routes.entity.*;
import devkor.com.teamcback.domain.routes.repository.CheckpointRepository;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.routes.repository.ShuttleTimeRepository;
import devkor.com.teamcback.domain.search.util.HangeulUtils;
import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.logging.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static devkor.com.teamcback.domain.routes.entity.Conditions.*;
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
    private final LogUtil logUtil;
    private final ShuttleTimeRepository shuttleTimeRepository;

    private static final long OUTDOOR_ID = 0L;
    private static final String SEPARATOR = ",";
    private static final Long INF = Long.MAX_VALUE;
    private static final Double INIT_OUTDOOR_DISTANCE = Double.MAX_VALUE;
    private static final Double MAX_OUTDOOR_DISTANCE = 0.003;
    private static final Double MIN_OUTDOOR_DISTANCE = 0.0001;
    private static final Double INDOOR_ROUTE_WEIGHT = 0.3;
    private static final Double BETWEEN_WEIGHT = 0.0005;

    //테스트용 TESTTIME
    private static final LocalTime TEST_TIME = LocalTime.of(14, 20, 0);

    /**
     * 메인 경로탐색 메서드
     */
    @Transactional(readOnly = true)
    public List<GetRouteRes> findRoute(LocationType startType, Long startId, Double startLat, Double startLong,
                                       LocationType endType, Long endId, Double endLat, Double endLong, List<Conditions> conditions) {

        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        conditions.add(OPERATING);
        Node startNode = getNodeByType(startType, startId, startLat, startLong);
        Node endNode = getNodeByType(endType, endId, endLat, endLong);

        // 에러 조건 확인
        checkLocationError(startNode, endNode, startType, endType, startId, endId);

        // 길찾기 응답 리스트 생성
        List<GetRouteRes> routeRes = new ArrayList<>();

        // 연결된 건물 찾기
        HashSet<Building> buildingList = getBuildingsForRoute(startNode, endNode, conditions);

        // 경로를 하나만 반환하는 경우
        GetGraphRes graphRes = getGraph(buildingList, startNode, endNode, conditions);
        DijkstraRes route = dijkstra(graphRes, startNode, endNode);

        routeRes.add(buildRouteResponse(route, startType == LocationType.BUILDING, endType == LocationType.BUILDING, conditions));

        // 베리어프리만 추가로 적용하는 경우(임시)
//        GetGraphRes graphRes2 = getGraph(buildingList, startNode, endNode, List.of(BARRIERFREE));
//        DijkstraRes route2 = dijkstra(graphRes2.getGraphNode(), graphRes2.getGraphEdge(), startNode, endNode);
//        if(route.getPath().isEmpty() && route2.getPath.isEmpty()) throw new GlobalException(NOT_FOUND_ROUTE);
//        routeRes.add(buildRouteResponse(route2, isStartBuilding, isEndBuilding));

        // 로그 저장
        Building startBuilding = null, endBuilding = null;
        Place startPlace = null, endPlace = null;

        if (checkType(startType, endType)) {
            if (startType == LocationType.BUILDING) startBuilding = findBuilding(startId);
            else if (startType == LocationType.PLACE) startPlace = findPlace(startId);

            if (endType == LocationType.BUILDING) endBuilding = findBuilding(endId);
            else if (endType == LocationType.PLACE) endPlace = findPlace(endId);

            logUtil.logRoute(startBuilding, startPlace, endBuilding, endPlace, conditions);
        }
        return routeRes;
    }

    private boolean checkType(LocationType startType, LocationType endType) {
        return (startType == LocationType.BUILDING || startType == LocationType.PLACE) && (endType == LocationType.BUILDING || endType == LocationType.PLACE);
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
        if (startNode.getBuilding().getId().equals(OUTDOOR_ID) && endNode.getBuilding().getId().equals(OUTDOOR_ID)) {
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
    private Node findNearestNode(Double latitude, Double longitude) {
        List<Node> graphNode = new ArrayList<>(findAllNode(findBuilding(OUTDOOR_ID)));
        double shortestDistance = INIT_OUTDOOR_DISTANCE;
        Node nearestNode = null;

        for (Node node : graphNode) {
            double thisDistance = getEuclidDistance(latitude, longitude, node.getLatitude(), node.getLongitude());
            if (nearestNode == null || shortestDistance > thisDistance) {
                nearestNode = node;
                shortestDistance = thisDistance;
            }
        }
        if (nearestNode == null) {
            throw new GlobalException(NOT_FOUND_NODE);
        }
        if (getEuclidDistance(latitude, longitude, nearestNode.getLatitude(), nearestNode.getLongitude()) > MAX_OUTDOOR_DISTANCE) {
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
    private HashSet<Building> getBuildingsForRoute(Node startNode, Node endNode, List<Conditions> conditions) {
        HashSet<Building> buildingList = new HashSet<>();
        buildingList.add(findBuilding(OUTDOOR_ID)); // 외부 경로 추가
        buildingList.add(startNode.getBuilding());
        buildingList.add(endNode.getBuilding());
        if (conditions.contains(INNERROUTE)) {
            Node startSearchNode = startNode;
            Node endSearchNode = endNode;
            if (!startNode.getBuilding().getId().equals(OUTDOOR_ID))
                startSearchNode = startNode.getBuilding().getNode();
            if (!endNode.getBuilding().getId().equals(OUTDOOR_ID)) endSearchNode = endNode.getBuilding().getNode();

            addInBetweenBuildings(startSearchNode, endSearchNode, buildingList);
        }
        HashSet<Building> buildingListCpy = new HashSet<>(buildingList);
        for (Building building : buildingListCpy) {
            addConnectedBuildings(building, buildingList);
        }

        return buildingList;
    }

    /**
     * 출발/도착지에 연결된 건물들이 있는 경우 buildingList에 추가하는 메서드
     * 연쇄적으로 연결된 건물들도 반영하도록(ex: 엘포관-백기-중지-SK미래관...) 수정
     */
    private void addConnectedBuildings(Building startBuilding, HashSet<Building> buildingList) {
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
     * 실내우선 경로 전용
     * 시작 도착지역 기준 사이 건물 추가 메서드
     */
    private void addInBetweenBuildings(Node startNode, Node endNode, HashSet<Building> buildingList) {
        Vector2D startPoint = new Vector2D(startNode.getLatitude(), startNode.getLongitude());
        Vector2D endPoint = new Vector2D(endNode.getLatitude(), endNode.getLongitude());
        List<Building> allBuildings = buildingRepository.findAll();
        Vector2D startToEnd = endPoint.subtract(startPoint);
        for (Building building : allBuildings) {
            if (building.getNode() != null) {
                Vector2D buildingPoint = new Vector2D(building.getNode().getLatitude(), building.getNode().getLongitude());
                Vector2D startToBuilding = buildingPoint.subtract(startPoint);
                double t = startToBuilding.dot(startToEnd) / startToEnd.normSquared();

                if (t > 0 && t < 1) {
                    double distance = Math.sqrt(startToBuilding.normSquared() - t * t * startToEnd.normSquared());
                    if (distance < BETWEEN_WEIGHT) buildingList.add(building);
                }
            }
        }


    }


    /**
     * 그래프 요소 찾기(node, edge 묶음)
     * 노드 테이블의 String 인접 노드와 거리를 그래프로 변환
     */
    private GetGraphRes getGraph(HashSet<Building> buildingList, Node startNode, Node endNode, List<Conditions> conditions) {
        List<Node> graphNode = new ArrayList<>();
        Map<Long, List<Edge>> graphEdge = new HashMap<>();

        // 조건에 따른 노드 검색

        for (Building building : buildingList) {
            if (conditions.contains(OPERATING)) {
                if (!building.isOperating()) continue;
            }
            graphNode.addAll(findNodeWithConditions(building, conditions));
        }

        if (!graphNode.contains(startNode)) {
            graphNode.add(startNode);
        }
        if (!graphNode.contains(endNode)) {
            graphNode.add(endNode);
        }

        for (Node node : graphNode) {
            String rawAdjacentNode = node.getAdjacentNode();
            String rawDistance = node.getDistance();

            if (rawAdjacentNode == null || rawDistance == null || rawAdjacentNode.isEmpty() || rawDistance.isEmpty())
                continue;

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

            if (!graphEdge.containsKey(node.getId())) {
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
    private List<Node> findNodeWithConditions(Building building, List<Conditions> conditions) {
        List<NodeType> nodeTypes = new ArrayList<>(Arrays.asList(NodeType.NORMAL, NodeType.STAIR, NodeType.ELEVATOR, NodeType.ENTRANCE, NodeType.CHECKPOINT));
        if (conditions != null) {
            if (conditions.contains(BARRIERFREE)) {
                nodeTypes.remove(NodeType.STAIR);
            } else if (conditions.contains(Conditions.SHUTTLE)) {
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
        Map<Long, Long> weights = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> priorityQueue = new PriorityQueue<>();
        Set<Long> visitedNodes = new HashSet<>();

        // 모든 노드 초기화
        for (Node node : nodes) {
            if (node.equals(startNode)) {
                distances.put(node.getId(), 0L);
                weights.put(node.getId(), 0L);
                priorityQueue.add(new NodeDistancePair(node.getId(), 0L));
            } else {
                distances.put(node.getId(), INF);
                weights.put(node.getId(), INF);
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
                Long currentWeight = weights.get(currentNode);
                if (currentWeight == null) continue;

                Long newWeight = currentWeight + edge.getWeight();  //weight 기반 탐색으로 수정
                Long newDistance = currentDistance + edge.getDistance();
                Long neighborWeight = weights.get(neighbor);

                if (neighborWeight == null || newWeight < neighborWeight && newWeight < INF) {
                    weights.put(neighbor, newWeight);
                    distances.put(neighbor, newDistance);
                    previousNodes.put(neighbor, currentNode);
                    priorityQueue.add(new NodeDistancePair(neighbor, newWeight));
                }
            }
        }

        //path 생성
        List<Node> path = new ArrayList<>();
        Long finalDistance = distances.get(endNode.getId());
        for (Long at = endNode.getId(); at != null; at = previousNodes.get(at)) {
            Node node = nodeRepository.findById(at).orElseThrow(() -> new GlobalException(NOT_FOUND_ROUTE));
            path.add(node);
        }
        Collections.reverse(path);

        //예외처리: path가 제대로 나오지 않는 경우. 즉, 경로가 존재하지 않는 경우
        if (path.isEmpty() || !path.get(0).equals(startNode)) {
            throw new GlobalException(NOT_FOUND_ROUTE);
        }

        return new DijkstraRes(finalDistance, path);
    }

    /**
     * 경로를 분할하고 응답 형식에 맞게 변환
     */
    private GetRouteRes buildRouteResponse(DijkstraRes route, boolean isStartBuilding, boolean isEndBuilding, List<Conditions> conditions) {
//        if (route.getPath().isEmpty()) return new GetRouteRes(1);//경로 미탐색 막기 위해 임의로 추가
        List<LocalTime> busTimeStamps = null;
        int busIdx = 0;
        if (conditions.contains(SHUTTLE)) busTimeStamps = modifyRoute(route);
        Long duration = route.getDistance();
        List<List<Node>> path = cutRoute(route.getPath()); // 분할된 경로

        //시작, 끝이 건물인 경우 해당 노드 지우기
        if (isStartBuilding) {
            // 첫번째 path의 길이에 따라 삭제 다르게 하기
            if (path.get(0).size() > 1) {
                path.get(0).remove(0);
            } else {
                path.remove(0);
            }
        }

        if (isEndBuilding && path.get(path.size() - 1).size() != 1) {
            path.get(path.size() - 1).remove(path.get(path.size() - 1).size() - 1);
        }

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
            //route 경로 설명을 추가하며, 셔틀경로가 있는 경우 waiting time 더하기
            if (i + 1 == path.size()) {
                partialRouteRes.setInfo(makeInfo(thisPath.get(thisPath.size() - 1), null, null));
            } else {
                if (busTimeStamps == null) {
                    partialRouteRes.setInfo(makeInfo(thisPath.get(thisPath.size() - 1), path.get(i + 1).get(0), null));
                }
                else{
                    String info = makeInfo(thisPath.get(thisPath.size() - 1), path.get(i + 1).get(0), busTimeStamps.get(busIdx));
                    if (info.contains("탑승하세요") && busIdx < busTimeStamps.size() - 1) busIdx++;
                    partialRouteRes.setInfo(info);
                }
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
        List<Place> busStops = placeRepository.findAllByType(PlaceType.SHUTTLE_BUS);


        while (count < route.size() - 1) {
            thisNode = route.get(count);
            nextNode = route.get(count + 1);

            // 새로운 건물로 이동할 때 & 체크포인트일때 & 외부에서 새로운 건물로 들어갈 때(입구 분리) & 셔틀버스 경로로 출입할때 경로분할
            if ((!thisNode.getBuilding().equals(nextNode.getBuilding()) && thisNode.getBuilding().getId() != OUTDOOR_ID)
                    || (thisNode.getType() != nextNode.getType() && thisNode.getType() == NodeType.CHECKPOINT)
                    || (thisNode.getBuilding().getId() == OUTDOOR_ID && nextNode.getType() == NodeType.ENTRANCE)
                    || (isBusStop(busStops, thisNode) && nextNode.getType() == NodeType.SHUTTLE)
                    || (thisNode.getType() != nextNode.getType() && thisNode.getType() == NodeType.SHUTTLE)) {
                partialRoute.add(thisNode);
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();
            }

            // 같은 건물 내에서 층 이동 시 경로 분할, 중간 층 생략
            else if (!Objects.equals(thisNode.getFloor(), nextNode.getFloor())) {
                partialRoute.add(thisNode);

                // 계단/엘리베이터를 통한 연속적인 층 이동을 감지하여 중간 층을 생략
                while (!Objects.equals(thisNode.getFloor(), nextNode.getFloor()) && thisNode.getBuilding().equals(nextNode.getBuilding())) {
                    count++;
                    thisNode = route.get(count);
                    nextNode = route.get(count+1);
                }
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();

                // 끝 층의 시작 노드를 새 경로로 추가
                count--;
            } else {
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
    private String makeInfo(Node prevNode, Node nextNode, LocalTime timeStamp) {
        List<Place> busStops = placeRepository.findAllByType(PlaceType.SHUTTLE_BUS);
        if (nextNode == null) return "도착";

        if (prevNode.getType() == NodeType.CHECKPOINT) {
            String checkpointName = findCheckpoint(prevNode).getName();
            return makeString(checkpointName);
        }
        //shuttle버스 탑승하는 경우
        if (isBusStop(busStops, prevNode) && nextNode.getType() == NodeType.SHUTTLE) {
            if (timeStamp == null) {
                return placeRepository.findByNode(prevNode).getDetail() + "에서 셔틀버스에 탑승하세요.";
            } else {
                return placeRepository.findByNode(prevNode).getDetail() + "에서 셔틀버스에 탑승하세요. (" + timeStamp.getHour() + "시 " + timeStamp.getMinute() + "분 버스)";
            }
        }

        //셔틀버스 내리는 경우
        if (prevNode.getType() != nextNode.getType() && prevNode.getType() == NodeType.SHUTTLE) {
            return placeRepository.findByNode(nextNode).getDetail() + "에서 셔틀버스에서 내리세요.";
        }

        Building prevNodeBuilding = prevNode.getBuilding();
        Building nextNodeBuilding = nextNode.getBuilding();
        // 건물 외부에서 내부로 들어가는 경우: prevNodeBuilding과 nextNodeBuilding이 같음(id=0L)
        // 미리 출입구에서 한 번 추가적으로 끊기 때문.
        if (Objects.equals(prevNodeBuilding, nextNodeBuilding) && prevNodeBuilding.getId() == OUTDOOR_ID) {
            Building enteringBuilding = findLinkedBuilding(nextNode);
            return makeString(enteringBuilding.getName());
        }

        int nextNodeFloor = nextNode.getFloor().intValue();
        String floor = nextNodeFloor >= 0 ? Integer.toString(nextNodeFloor) : "B" + Math.abs(nextNodeFloor);
        // 그 외 건물이 같은 경우는 층 이동의 경우밖에 없음
        if (Objects.equals(prevNodeBuilding, nextNodeBuilding)) {
            return floor + "층으로 이동하세요.";
        }
        // 끊긴 출입구 기준으로 바깥에서 안으로 들어가는 경우
        if (prevNodeBuilding.getId() == OUTDOOR_ID) {
            return nextNodeBuilding.getName() + " " + floor + "층 출입구로 들어가세요.";
        }
        // 안에서 바깥으로 나가는 경우
        if (nextNodeBuilding.getId() == OUTDOOR_ID) return "출입구를 통해 밖으로 나가세요";
        // 건물 사이를 바로 이동하는 경우
        return "출입구를 통해 " + nextNodeBuilding.getName() + " " + floor + "층으로 이동하세요.";
    }


    /**
     * makeinfo에서 활용하는 로/으로 구분 메서드
     */
    private String makeString(String name) {
        String decomposedName = HangeulUtils.decomposeHangulString(name);
        if (decomposedName.endsWith("ㄹ") || !HangeulUtils.isConsonantOnly(decomposedName.substring(decomposedName.length() - 1))) {
            return name + "로 이동하세요.";
        } else {
            return name + "으로 이동하세요.";
        }
    }

    /**
     * 노드에 연결된 건물을 찾는 메서드(들어오는 예상 노드는 ENTRANCE).
     * 건물 내부가 완료되지 않아 / 가장 가까운 출입구가 출입금지라 entrance와 연결된 내부 노드가 없을 수도 있는데, 이 경우 건물 노드로 찾는다
     */
    private Building findLinkedBuilding(Node node) {
        Long[] adjacentNodeIds = convertStringToArray(node.getAdjacentNode());
        return buildingRepository.findByNodeIdIn(adjacentNodeIds)
                .orElseThrow(() -> new AdminException(INCORRECT_NODE_DATA, node.getId() + "번 노드에 연결된 건물이 없습니다"));
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

    //셔틀버스용 메서드들

    /**
     * 특정 place들이 전부 bus stop인지 확인하는 메서드
     */
    private boolean isBusStop(List<Place> busStops, Node node) {
        for (Place busStop : busStops) {
            if (busStop.getNode() == node) return true;
        }
        return false;
    }

    /**
     * (대략적으로) 학기중이면 0, 여름방학이면 1, 겨울방학이면 2 리턴 메서드
     */
    private int summerSession() {
        int now = LocalDate.now().getMonthValue();
        return switch (now) {
            case 3, 4, 5, 6, 9, 10, 11, 12 -> 0;
            case 7, 8 -> 1;
            default -> 2;
        };
    }

    /**
     * 특정 노드(반드시 셔틀버스 정류장)에서 현재 시간 + distance기준으로 몇 분 버스에 타야 하는지 리턴하는 메서드
     */
    private LocalTime calculateBusTime(Long distance, Node node, LocalTime currentTime) {
        boolean isSummerSession = summerSession() != 0;
        //테스트용 summersession 코드
        //boolean isSummerSession = false;
        LocalTime startTime = currentTime.plusSeconds(distance);
        Place busStop = placeRepository.findByNode(node);
        List<ShuttleTime> busStopSchedule = shuttleTimeRepository.findAllByPlaceAndSummerSession(busStop, isSummerSession, Sort.by(Sort.Direction.ASC, "time"));
        for (ShuttleTime shuttleTime : busStopSchedule) {
            LocalTime thisTime = shuttleTime.getTime();
            if (startTime.isBefore(thisTime)) {
                return thisTime;
            }
        }
        return null;
    }

    /**
     * 현재 버스 운영 시간인지 리턴
     */
    private boolean isBusTime() {
        int semester = summerSession();
        boolean isSemester;
        if (semester == 2) return false;
        else isSemester = (semester == 0);
        LocalTime now = LocalTime.now();
        //테스트용 now 설정
        //LocalTime now = TEST_TIME;
        List<ShuttleTime> busStopSchedule = shuttleTimeRepository.findAllBySummerSession(isSemester, Sort.by(Sort.Direction.ASC, "time"));
        LocalTime firstTime = busStopSchedule.get(0).getTime();
        LocalTime lastTime = busStopSchedule.get(busStopSchedule.size() - 1).getTime();
        return (now.isAfter(firstTime.minusMinutes(20)) && now.isBefore(lastTime.minusMinutes(20)));
    }

    /**
     * 셔틀버스용 생성된 경로 수정
     */
    private List<LocalTime> modifyRoute(DijkstraRes route) {
        boolean isShuttleRoute = false;
        List<Node> path = route.getPath();
        //버스를 2번 이상 탑승 가능하므로 list형태로 저장
        List<Integer> shuttleIdx = new ArrayList<>();
        List<LocalTime> busTimes = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            if (path.get(i).getType() != NodeType.SHUTTLE && path.get(i + 1).getType() == NodeType.SHUTTLE) {
                isShuttleRoute = true;
                shuttleIdx.add(i);
            }
        }
        //셔틀을 이용한 경로가 반환되지 않았을 경우 NOT_FOUND_ROUTE
        if (!isShuttleRoute) throw new GlobalException(NOT_FOUND_ROUTE);
        Long shuttleWaitTime = 0L;
        if (isBusTime()) {
            LocalTime currentTime = LocalTime.now();
            //테스트용 currentTime 설정
            //LocalTime currentTime = TEST_TIME;
            for (Integer idx : shuttleIdx) {
                Long distToShuttle = countDistance(path, idx) + shuttleWaitTime;
                LocalTime busTime = calculateBusTime(distToShuttle, path.get(idx), currentTime);
                busTimes.add(busTime);
                shuttleWaitTime += Duration.between(currentTime, busTime).getSeconds();
                route.setDistance(route.getDistance() + Duration.between(currentTime, busTime).getSeconds());
            }
            return busTimes;
        } else {
            route.setDistance(route.getDistance() + 300);
            return null;
        }
    }

    private Long countDistance(List<Node> route, int shuttleIdx) {
        long distance = 0L;
        for (int i = 0; i < shuttleIdx; i++) {
            String[] adjNodes = route.get(i).getAdjacentNode().split(",");
            String[] adjDists = route.get(i).getDistance().split(",");
            for (int j = 0; j < adjNodes.length; j++) {
                if (Long.parseLong(adjNodes[j]) == route.get(i + 1).getId()) {
                    distance += Long.parseLong(adjDists[j]);
                    break;
                }
            }
        }
        return distance;
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

    private List<Node> findAllNode(Building building) {
        return nodeRepository.findByBuildingAndRouting(building, true);
    }

    private Checkpoint findCheckpoint(Node node) {
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

    /**
     * 벡터 계산용 클래스
     */
    public class Vector2D {
        private double x;
        private double y;

        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        //벡터 덧셈
        public Vector2D add(Vector2D vector2D) {
            return new Vector2D(this.x + vector2D.x, this.y + vector2D.y);
        }

        //벡터 뺄셈
        public Vector2D subtract(Vector2D vector2D) {
            return new Vector2D(this.x - vector2D.x, this.y - vector2D.y);
        }

        //내적
        public double dot(Vector2D vector2D) {
            return this.x * vector2D.x + this.y * vector2D.y;
        }

        //벡터의 제곱 크기
        public double normSquared() {
            return this.x * this.x + this.y * this.y;
        }

        @Override
        public String toString() {
            return String.format("Vector2D(%.2f, %.2f)", this.x, this.y);
        }
    }
}



