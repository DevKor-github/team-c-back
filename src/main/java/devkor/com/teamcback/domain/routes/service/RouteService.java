package devkor.com.teamcback.domain.routes.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.dto.response.DijkstraRes;
import devkor.com.teamcback.domain.routes.dto.response.GetGraphRes;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.routes.entity.*;
import devkor.com.teamcback.domain.routes.repository.CheckpointRepository;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.*;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final NodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final CheckpointRepository checkpointRepository;

    /**
     * 메인 경로탐색 메서드
     */
    @Transactional(readOnly = true)
    public GetRouteRes findRoute(List<Double> startPosition, LocationType startType,
        List<Double> endPosition, LocationType endType, NodeType barrierFree) throws ParseException {

        Node startNode = getNodeByType(startPosition, startType);
        Node endNode = getNodeByType(endPosition, endType);

        boolean isStartBuilding = startType == LocationType.BUILDING;
        boolean isEndBuilding = endType == LocationType.BUILDING;

        // 에러 조건 확인
        if (isStartBuilding && isEndBuilding) {
            if (startPosition.get(0).longValue() == endPosition.get(0).longValue()) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        if (isStartBuilding && endType == LocationType.PLACE) {
            if (startPosition.get(0).longValue() == endNode.getBuilding().getId()) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        if (isEndBuilding && startType == LocationType.PLACE) {
            if (startNode.getBuilding().getId() == endPosition.get(0).longValue()) {
                throw new GlobalException(NOT_PROVIDED_ROUTE);
            }
        }

        List<Building> buildingList = getBuildingsForRoute(startNode, endNode);
        GetGraphRes graphRes = getGraph(buildingList, startNode, endNode, barrierFree);
        DijkstraRes route = dijkstra(graphRes.getGraphNode(), graphRes.getGraphEdge(), startNode, endNode);
        if (route.getPath().isEmpty()) return new GetRouteRes(1);
        return buildRouteResponse(route, isStartBuilding, isEndBuilding);
    }

    /**
     * 탐색 알고리즘의 효율성을 위해 이동할 만한 건물들만 추리는 메서드
     */
    private List<Building> getBuildingsForRoute(Node startNode, Node endNode) {
        List<Building> buildingList = new ArrayList<>();
        buildingList.add(findBuilding(0L)); // 외부 경로 추가
        buildingList.add(startNode.getBuilding());
        buildingList.add(endNode.getBuilding());

        addLinkedBuildings(startNode.getBuilding(), buildingList);
        addLinkedBuildings(endNode.getBuilding(), buildingList);

        return buildingList;
    }

    /**
     * 출발/도착지에 직접적으로 연결된 건물들이 있는 경우 buildingList에 추가하는 메서드
     */
    private void addLinkedBuildings(Building building, List<Building> buildingList) {
        List<Long> linkedBuildingIds = LinkedBuildingData.getLinkedBuildings(building.getId());
        for (Long linkedBuildingId : linkedBuildingIds) {
            Building linkedBuilding = findBuilding(linkedBuildingId);
            if (!buildingList.contains(linkedBuilding)) {
                buildingList.add(linkedBuilding);
            }
        }
    }

    /**
     * 경로 생성 메서드
     */
    private GetRouteRes buildRouteResponse(DijkstraRes route, boolean isStartBuilding, boolean isEndBuilding) {
        Long duration = route.getDistance();
        List<List<Node>> path = cutRoute(route.getPath(), isStartBuilding, isEndBuilding);
        List<PartialRouteRes> totalRoute = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            List<Node> thisPath = path.get(i);
            boolean isOutside = thisPath.get(0).getBuilding().getId() == 0L;
            List<List<Double>> partialRoute = convertNodesToCoordinates(thisPath, isOutside);
            Long buildingId = isOutside ? 0L : thisPath.get(0).getBuilding().getId();
            Double floor = thisPath.get(0).getFloor();

            PartialRouteRes partialRouteRes = isOutside
                ? new PartialRouteRes(partialRoute)
                : new PartialRouteRes(buildingId, floor, partialRoute);

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
     * 타입에 맞는 Node 찾기
     */
    private Node getNodeByType(List<Double> position, LocationType type) {
        return switch (type) {
            case COORD -> findNearestNode(position.get(0), position.get(1));
            case BUILDING -> findBuilding(position.get(0).longValue()).getNode();
            case PLACE -> findPlace(position.get(0).longValue()).getNode();
            case NODE -> findNode(position.get(0).longValue());
            default -> throw new IllegalArgumentException("Invalid LocationType: " + type);
        };
    }

    /**
     * 그래프 요소 찾기(node, edge 묶음)
     */
    private GetGraphRes getGraph(List<Building> buildingList, Node startNode, Node endNode, NodeType nodeToBan){
        List<Node> graphNode = new ArrayList<>();
        List<Edge> graphEdge = new ArrayList<>();
        if (nodeToBan == null){
            for (Building i : buildingList){
                graphNode.addAll(findAllNode(i));
            }
        }
        else{
            for (Building i : buildingList){
                graphNode.addAll(findNodeWithExceptions(i, nodeToBan));
            }
        }
        if (!startNode.isOperating() || !endNode.isOperating()) throw new GlobalException(NOT_OPERATING);
        if (!graphNode.contains(startNode)){
            graphNode.add(startNode);
        }
        if (!graphNode.contains(endNode)){
            graphNode.add(endNode);
        }
        for (Node node: graphNode){
            String rawAdjacentNode = node.getAdjacentNode();
            String rawDistance = node.getDistance();
            //추후 수정이 필요할 코드(에러핸들링)
            if (rawAdjacentNode == null || rawDistance == null || rawAdjacentNode.isEmpty() || rawDistance.isEmpty()) continue;
            String[] endNodeId = node.getAdjacentNode().split(",");
            String[] distance = node.getDistance().split(",");
            if (endNodeId.length != distance.length) throw new Error("노드"+node.getId()+"에 형식의 문제가 있습니다.");
            //if (endNodeId.length != distance.length) continue;

            for (int i = 0; i < endNodeId.length; i++) {
                // 시작끝 모두 Long으로 써서 경로 찾은 후, 해당 경로 대해서만 node 찾기
                graphEdge.add(new Edge(Integer.parseInt(distance[i]), node.getId(), Long.parseLong(endNodeId[i])));
            }
        }
        return new GetGraphRes(graphNode, graphEdge);
    }

    /**
     * 주어진 좌표값에서 가장 가까운 외부 노드 찾기
     */
    private Node findNearestNode(Double latitude, Double longitude){
        List<Node> graphNode = new ArrayList<>(
            findAllNode(findBuilding(0L)));
        double shortestDistance = 99999;
        Node nearestNode = null;

        for (Node i: graphNode){
            double thisDistance = getEuclidDistance(latitude, longitude, i.getLatitude(), i.getLongitude());
            if (nearestNode == null || shortestDistance > thisDistance){
                nearestNode = i;
                shortestDistance = thisDistance;
            }
        }
        if (getEuclidDistance(latitude, longitude, nearestNode.getLatitude(), nearestNode.getLongitude()) > 0.003){
            throw new GlobalException(COORDINATES_TOO_FAR);
        }
        return nearestNode;
    }

    private double getEuclidDistance(double startX, double startY, double endX, double endY) {
        return Math.sqrt(Math.pow((startX - endX), 2) + Math.pow((startY - endY), 2));
    }

    /**
     * 전체 경로 리스트를 받아 층별, 건물별로 끊기
     */
    private List<List<Node>> cutRoute(List<Node> route, boolean isStartBuilding, boolean isEndBuilding) {
        if (route.isEmpty()) throw new Error("파싱할 리스트가 비었습니다.");

        List<List<Node>> returnRoute = new ArrayList<>();
        List<Node> partialRoute = new ArrayList<>();
        int count = 0;
        Node thisNode;
        Node nextNode = null;


        while (count < route.size() - 1) {
            thisNode = route.get(count);
            nextNode = route.get(count + 1);

            // 새로운 건물로 이동할 때 경로 분할
            if ((!Objects.equals(thisNode.getBuilding(), nextNode.getBuilding())) || (thisNode.getType() != nextNode.getType() && thisNode.getType() == NodeType.CHECKPOINT)) {
                partialRoute.add(thisNode);
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();
            }
            // 같은 건물 내에서 층 이동 시 경로 분할, 중간 층 생략
            else if (!Objects.equals(thisNode.getFloor(), nextNode.getFloor())) {
                partialRoute.add(thisNode);
                // 계단/엘리베이터를 통한 연속적인 층 이동을 감지하여 중간 층을 생략
                while (count < route.size() - 1 && !Objects.equals(thisNode.getFloor(), nextNode.getFloor())) {
                    count++;
                    thisNode = route.get(count);
                    nextNode = route.get(count + 1);
                }
                returnRoute.add(new ArrayList<>(partialRoute));
                partialRoute.clear();
                partialRoute.add(thisNode);  // 끝 층의 시작 노드를 새 경로로 추가
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
     * 나눠진 기준으로 두 노드를 받아 비교하여 간단한 설명 생성
     * nextNode에 null이 들어오면 경로 안내가 끝난 상황이라고 판단
     */
    private String makeInfo(Node prevNode, Node nextNode){
        if (nextNode == null) return "도착";

        Building prevNodeBuilding = prevNode.getBuilding();
        Building nextNodeBuilding = nextNode.getBuilding();
        int nextNodeFloor = nextNode.getFloor().intValue();
        String floor = nextNodeFloor >= 0 ? Integer.toString(nextNodeFloor) : "B" + Math.abs(nextNodeFloor);

        //건물이 같은 경우는 층 이동의 경우밖에 없음
        if (prevNode.getType() == NodeType.CHECKPOINT){
            String checkpointName = findCheckpoint(prevNode).getName();
            return checkpointName + "(으)로 이동하세요.";
        }
        else if (Objects.equals(prevNodeBuilding, nextNodeBuilding)){
            return floor + "층으로 이동하세요.";
        }
        //바깥에서 안으로 들어가는 경우
        else if (prevNodeBuilding.getId() == 0L){
            return nextNodeBuilding.getName() + " " + floor + "층 출입구로 들어가세요.";
        }
        //안에서 바깥으로 나가는 경우
        else{
            if (nextNodeBuilding.getId() == 0L) return "출입구를 통해 밖으로 나가세요";
            else return "출입구를 통해 " + nextNodeBuilding.getName() + " " + floor + "층으로 이동하세요.";
        }
    }


    // findEntity 메서드
    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private List<Node> findAllNode(Building building){
        return nodeRepository.findByBuildingAndRouting(building, true);
    }

    private List<Node> findNodeWithExceptions(Building building, NodeType nodeToBan){
        return nodeRepository.findByBuildingAndRoutingAndTypeNot(building, true, nodeToBan);
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private Checkpoint findCheckpoint(Node node){
        return checkpointRepository.findByNode(node);
    }

    //다익스트라용 메서드
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
    private DijkstraRes dijkstra(List<Node> nodes, List<Edge> edges, Node startNode, Node endNode) {
        Map<Long, Long> distances = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> priorityQueue = new PriorityQueue<>();
        Set<Long> visitedNodes = new HashSet<>();

        // 모든 노드를 초기화합니다.
        for (Node node : nodes) {
            if (node.equals(startNode)) {
                distances.put(node.getId(), 0L);
                priorityQueue.add(new NodeDistancePair(node.getId(), 0L));
            } else {
                distances.put(node.getId(), Long.MAX_VALUE);
            }
            previousNodes.put(node.getId(), null);
        }

        while (!priorityQueue.isEmpty()) {
            NodeDistancePair currentPair = priorityQueue.poll();
            Long currentNode = currentPair.node;

            if (!visitedNodes.add(currentNode)) {
                continue;
            }

            if (currentNode.equals(endNode.getId())) {
                break;
            }

            for (Edge edge : edges) {
                if (edge.getStartNode().equals(currentNode)) {
                    Long neighbor = edge.getEndNode();
                    Long currentDistance = distances.get(currentNode);
                    if (currentDistance == null) {
                        continue; // currentNode가 distances에 존재하지 않는 경우를 대비
                    }
                    Long newDist = currentDistance + edge.getDistance();

                    Long neighborDist = distances.get(neighbor);
                    if (neighborDist == null || newDist < neighborDist) {
                        distances.put(neighbor, newDist);
                        previousNodes.put(neighbor, currentNode);
                        priorityQueue.add(new NodeDistancePair(neighbor, newDist));
                    }
                }
            }
        }

        List<Node> path = new ArrayList<>();
        Long finalDistance = distances.get(endNode.getId());
        if (finalDistance == Long.MAX_VALUE) {
            return new DijkstraRes(-1L, Collections.emptyList()); // 경로가 존재하지 않을 때
        }

        for (Long at = endNode.getId(); at != null; at = previousNodes.get(at)) {
            Optional<Node> node = nodeRepository.findById(at);
            node.ifPresent(path::add);
        }
        Collections.reverse(path);

        return new DijkstraRes(finalDistance, path);
    }

}