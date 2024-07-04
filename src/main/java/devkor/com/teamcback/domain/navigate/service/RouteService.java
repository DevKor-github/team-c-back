package devkor.com.teamcback.domain.navigate.service;

import static devkor.com.teamcback.global.response.ResultCode.*;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.dto.response.DijkstraRes;
import devkor.com.teamcback.domain.navigate.dto.response.GetGraphRes;
import devkor.com.teamcback.domain.navigate.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.navigate.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.navigate.entity.Edge;
import devkor.com.teamcback.domain.navigate.entity.LocationType;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.entity.NodeType;
import devkor.com.teamcback.domain.navigate.repository.EdgeRepository;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;
    private final FacilityRepository facilityRepository;

    @Transactional(readOnly = true)
    public GetRouteRes findRoute(List<Double> startPosition, LocationType startType,
        List<Double> endPosition, LocationType endType, NodeType barrierFree) throws ParseException {
        List<Building> buildingList = new ArrayList<>();
        //무슨 경로를 탐색하든 외부 경로는 항상 추가(같은 건물 탐색도 나갔다와야하는 경우가 있을 수 있음)
        buildingList.add(findBuilding(0L));

        Node startNode = getNodeByType(startPosition, startType);
        Node endNode = getNodeByType(endPosition, endType);

        //묶여있는 건물 탐색 및 추가 기능 여기에 필요
        buildingList.add(startNode.getBuilding());
        buildingList.add(endNode.getBuilding());

        //barrierFree 없는 경우 null로 들어옴, 있는 경우 계단 or 엘리베이터 들어옴.
        GetGraphRes graphRes = getGraph(buildingList, startNode, endNode, barrierFree);

        DijkstraRes route = dijkstra(graphRes.getGraphNode(), graphRes.getGraphEdge(), startNode, endNode);
        Long duration = route.getDistance();

        List<List<Node>> path = cutRoute(route.getPath());

        List<PartialRouteRes> totalRoute = new ArrayList<>();

        int pathSize = path.size();

        for (int i = 0; i < pathSize; i++) {
            List<Node> thisPath = path.get(i);
            List<List<Double>> partialRoute = new ArrayList<>();
            Long buildingId = thisPath.get(0).getBuilding().getId();
            double floor = thisPath.get(0).getFloor();
            if (buildingId == 0L){
                for (Node j: thisPath){
                    //디버깅 위해 route에 죄다 id넣어놓음. 나중에는 사라질 거기도 하고 마지막 index라 프론트쪽에서도 신경 안 써도 될듯.
                    partialRoute.add(Arrays.asList(j.getLatitude(), j.getLongitude(), j.getId().doubleValue()));
                }
                totalRoute.add(new PartialRouteRes(partialRoute));
            }
            else {
                for (Node j: thisPath){
                    partialRoute.add(Arrays.asList(j.getXCoord(), j.getYCoord(), j.getId().doubleValue()));
                }
                totalRoute.add(new PartialRouteRes(buildingId, floor, partialRoute));
            }

            if (i + 1 == pathSize) {
                totalRoute.get(i).setInfo(makeInfo(path.get(i).get(0), null));
            }
            else{
                totalRoute.get(i).setInfo(makeInfo(path.get(i).get(0), path.get(i+1).get(0)));
            }
        }

        return new GetRouteRes(duration, totalRoute);
    }

    /**
     * 타입에 맞는 Node 찾기
     */
    private Node getNodeByType(List<Double> position, LocationType type) {
        return switch (type) {
            case COORD -> findNearestNode(position.get(0), position.get(1));
            case BUILDING -> findBuilding(position.get(0).longValue()).getNode();
            case FACILITY -> findFacility(position.get(0).longValue()).getNode();
            case CLASSROOM -> findClassroom(position.get(0).longValue()).getNode();
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
        if (!graphNode.contains(startNode)){
            graphNode.add(startNode);
        }
        if (!graphNode.contains(endNode)){
            graphNode.add(endNode);
        }
        for (Node i: graphNode){
            graphEdge.addAll(findEdge(i));
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

        return nearestNode;
    }

    /**
     * 전체 경로 리스트를 받아 층별, 건물별로 끊기
     */
    private List<List<Node>> cutRoute(List<Node> route) {
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
            if (!Objects.equals(thisNode.getBuilding(), nextNode.getBuilding())) {
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
                partialRoute.add(thisNode);//층이 같아지는 시점부터 다음 경로에 넣기
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
     * 나눠진 기준으로 두 노드를 받아 비교하여 간단한 설명 생성
     * nextNode에 null이 들어오면 경로 안내가 끝난 상황이라고 판단
     */
    private String makeInfo(Node prevNode, Node nextNode){
        if (nextNode == null) return "도착";

        Building prevNodeBuilding = prevNode.getBuilding();
        Building nextNodeBuilding = nextNode.getBuilding();
        Double nextNodeFloor = nextNode.getFloor();

        //건물이 같은 경우는 층 이동의 경우밖에 없음
        if (Objects.equals(prevNodeBuilding, nextNodeBuilding)){
            return nextNodeFloor.toString() + "층으로 이동하세요.";
        }
        //바깥에서 안으로 들어가는 경우
        else if (prevNodeBuilding.getId() == 0L){
            return nextNodeBuilding.getName() + " " + nextNodeFloor.toString() + "층 출입구로 들어가세요.";
        }
        //안에서 바깥으로 나가는 경우
        else{
            if (nextNodeBuilding.getId() == 0L) return "출입구를 통해 밖으로 나가세요";
            else return "출입구를 통해 " + nextNodeBuilding.getName() + " " + nextNodeFloor.toString() + "층으로 이동하세요.";
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

    private List<Edge> findEdge(Node startNode){
        return edgeRepository.findByStartNode(startNode);
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId).orElseThrow(() -> new GlobalException(NOT_FOUND_CLASSROOM));
    }

    private Facility findFacility(Long facilityId) {
        return facilityRepository.findById(facilityId).orElseThrow(() -> new GlobalException(NOT_FOUND_FACILITY));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private double getEuclidDistance(double startX, double startY, double endX, double endY) {
        return Math.pow((startX - endX), 2) + Math.pow((startY - endY), 2);
    }

    //다익스트라용 메서드
    public static class NodeDistancePair implements Comparable<NodeDistancePair> {
        Node node;
        Long distance;

        public NodeDistancePair(Node node, Long distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistancePair other) {
            return Long.compare(this.distance, other.distance);
        }
    }
    public static DijkstraRes dijkstra(List<Node> nodes, List<Edge> edges, Node startNode, Node endNode) {
        Map<Node, Long> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> priorityQueue = new PriorityQueue<>();
        Set<Node> visitedNodes = new HashSet<>();

        // 모든 노드를 초기화합니다.
        for (Node node : nodes) {
            if (node.equals(startNode)) {
                distances.put(node, 0L);
                priorityQueue.add(new NodeDistancePair(node, 0L));
            } else {
                distances.put(node, Long.MAX_VALUE);
            }
            previousNodes.put(node, null);
        }

        while (!priorityQueue.isEmpty()) {
            NodeDistancePair currentPair = priorityQueue.poll();
            Node currentNode = currentPair.node;

            if (!visitedNodes.add(currentNode)) {
                continue;
            }

            if (currentNode.equals(endNode)) {
                break;
            }

            for (Edge edge : edges) {
                if (edge.getStartNode().equals(currentNode)) {
                    Node neighbor = edge.getEndNode();
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
        Long finalDistance = distances.get(endNode);
        if (finalDistance == Long.MAX_VALUE) {
            return new DijkstraRes(-1L, Collections.emptyList()); // 경로가 존재하지 않을 때
        }

        for (Node at = endNode; at != null; at = previousNodes.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        return new DijkstraRes(finalDistance, path);
    }

}
