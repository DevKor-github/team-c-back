package devkor.com.teamcback.domain.navigate.service;

import static devkor.com.teamcback.global.response.ResultCode.*;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingEntrance;
import devkor.com.teamcback.domain.building.repository.BuildingEntranceRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.dto.response.InnerRouteRes;
import devkor.com.teamcback.domain.navigate.dto.response.OuterRouteRes;
import devkor.com.teamcback.domain.navigate.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.navigate.dto.response.PathRes;
import devkor.com.teamcback.domain.navigate.entity.Edge;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.EdgeRepository;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.transport.entity.Transport;
import devkor.com.teamcback.domain.transport.entity.TransportFloor;
import devkor.com.teamcback.domain.transport.repository.TransportFloorRepository;
import devkor.com.teamcback.domain.transport.repository.TransportRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingEntranceRepository buildingEntranceRepository;
    private final ClassroomRepository classroomRepository;
    private final TransportRepository transportRepository;
    private final TransportFloorRepository transportFloorRepository;
    private final FacilityRepository facilityRepository;
    private final int INF_DURATION = 100_000;


    // 1. 강의실 <-> 강의실
    // 1-1. 같은 건물의 강의실인 경우 : 강의실1 -> 강의실2
    // 1-2. 다른 건물의 강의실인 경우 : 강의실1 -> 건물1 출입구 -> 건물2 출입구 -> 강의실2

    // 2. 건물 <-> 강의실
    // 2-1. 같은 건물의 강의실인 경우 : 건물의 출입구와 강의실 사이의 거리? TODO: 어떻게 처리할 것인지 논의 필요
    // 2-2. 다른 건물의 강의실인 경우 : 강의실과 가장 가까운 건물의 출입구 사이의 거리

    // 3. 건물 <-> 건물
    // 3-1. 같은 건물인 경우 -> TODO: 어떻게 처리할 것인지 논의 필요
    // 3-2. 다른 건물인 경우 : 건물 간 가장 가까운 출입구 찾기

    @Transactional(readOnly = true)
    public GetRouteRes findRoute(Long startBuildingId, Long startRoomId, PlaceType startType,
        Long endBuildingId, Long endRoomId, PlaceType endType) throws ParseException {
        GetRouteRes resDto = new GetRouteRes();

        // 건물
        Building startBuilding = findBuilding(startBuildingId);
        Building endBuilding = findBuilding(endBuildingId);

        // 건물 출입구
        BuildingEntrance startBuildingEntrance = findNearestEntrance(startBuilding, endBuilding);
        BuildingEntrance endBuildingEntrance = findNearestEntrance(endBuilding, startBuilding);

        // 강의실 노드 설정
        String startName = startBuilding.getName();
        Node startNode = null;
        String endName = endBuilding.getName();
        Node endNode = null;

        if (startRoomId != null) {
            NodeInfo nodeInfo = getNodeInfo(startType, startRoomId);
            startName = nodeInfo.name;
            startNode = nodeInfo.node;
        }

        if (endRoomId != null) {
            NodeInfo nodeInfo = getNodeInfo(endType, endRoomId);
            endName = nodeInfo.name;
            endNode = nodeInfo.node;
        }

        // 같은 건물인 경우
        if (startBuildingId.equals(endBuildingId)) {
            // 건물 <-> 건물인 경우
            if(startRoomId == null && endRoomId == null) {
                throw new GlobalException(NOT_FOUND_ROUTE);
            }

            InnerRouteRes innerRoute;

            // 건물 -> 강의실인 경우
            if(startRoomId == null) {
                innerRoute = getRouteInBuilding(startBuilding, startBuildingEntrance.getNode(), endNode);
            }
            // 강의실 -> 건물인 경우
            else if(endRoomId == null) {
                innerRoute = getRouteInBuilding(startBuilding, startNode, endBuildingEntrance.getNode());
            }
            // 강의실 <-> 강의실인 경우
            else {
                innerRoute = getRouteInBuilding(startBuilding, startNode, endNode);
            }

            innerRoute.setStartNodeName(startName);
            innerRoute.setEndNodeName(endName);
            resDto.setInnerRoute1(innerRoute);
        }

        // 다른 건물인 경우
        else{
            // 강의실1 -> 건물1 출입구
            if (startRoomId != null) {
                InnerRouteRes innerRoute = getRouteInBuilding(startBuilding, startNode, startBuildingEntrance.getNode());
                innerRoute.setStartNodeName(startName);
                innerRoute.setEndNodeName(startBuilding.getName());
                resDto.setInnerRoute1(innerRoute);
            }

            // 건물1 출입구 -> 건물2 출입구
            resDto.setOuterRoute(getOuterRoute(startBuildingEntrance, endBuildingEntrance));

            // 건물2 출입구 -> 강의실2
            if (endRoomId != null) {
                InnerRouteRes innerRoute = getRouteInBuilding(startBuilding, endBuildingEntrance.getNode(), endNode);
                innerRoute.setStartNodeName(endBuilding.getName());
                innerRoute.setEndNodeName(endName);
                resDto.setInnerRoute2(innerRoute);
            }
        }

        return resDto;
    }

    private NodeInfo getNodeInfo(PlaceType type, Long id) {
        String name;
        Node node;
        if(type == PlaceType.CLASSROOM) { // 강의실인 경우
            Classroom classroom = findClassroom(id);
            name = classroom.getName();
            node = classroom.getNode();
        }
        else { // 편의시설인 경우
            Facility facility = findFacility(id);
            name = facility.getName();
            node = facility.getNode();
        }

        return new NodeInfo(name, node);
    }

    private static class NodeInfo {
        private String name;
        private Node node;

        public NodeInfo(String name, Node node) {
            this.name = name;
            this.node = node;
        }
    }

    // 같은 건물 내에서의 최단 경로
    private InnerRouteRes getRouteInBuilding(Building building, Node startNode, Node endNode) {
        int startFloor = startNode.getFloor();
        int endFloor = endNode.getFloor();

        Integer minDuration = INF_DURATION;
        List<PathRes> shortestPath = new ArrayList<>();

        // 층이 같은 경우
        if(startFloor == endFloor) {
            // 출발지와 목적지가 같은 경우
            if(startNode.getId().equals(endNode.getId())) throw new GlobalException(NOT_FOUND_ROUTE);

            RouteOnFloor routeOnFloor = getRouteOnFloor(building, startFloor, startNode, endNode);
            minDuration += routeOnFloor.duration;
            shortestPath.addAll(routeOnFloor.path);

            return new InnerRouteRes(startNode, endNode, minDuration, shortestPath);
        }

        // 층이 다른 경우
        int diff = startFloor <= endFloor ? 1 : -1;

        // 해당 건물에 존재하는 이동 수단
        List<Transport> transportList = transportRepository.findAllByBuilding(building);

        // 최적의 이동 수단 1개만 고르는 방식으로 구현
        for(Transport transport : transportList) {
            Integer duration = 0;
            List<PathRes> path = new ArrayList<>();
            int i = startFloor;
            while ((diff > 0 && i <= endFloor) || (diff < 0 && i >= endFloor)) {
                TransportFloor transportFloor = transportFloorRepository.findByTransportAndFloor(transport, i);
                if(transportFloor == null) continue;

                RouteOnFloor routeOnFloor;
                if(i == startFloor) {
                    routeOnFloor = getRouteOnFloor(building, i, startNode, transportFloor.getNode());
                    duration += routeOnFloor.duration;
                    path.addAll(routeOnFloor.path);
                }
                else if(i == endFloor) {
                    routeOnFloor = getRouteOnFloor(building, i, transportFloor.getNode(), endNode);
                    duration += routeOnFloor.duration;
                    path.addAll(routeOnFloor.path);
                }
                else {
                    duration += transport.getType().getDuration();
                    path.add(new PathRes(transportFloor.getNode()));
                }
                i += diff;
            }

            if(duration < minDuration) {
                minDuration = duration;
                shortestPath = path;
            }
        }

        return new InnerRouteRes(startNode, endNode, minDuration, shortestPath);
    }

    private static class RouteOnFloor {
        Integer duration;
        List<PathRes> path;

        RouteOnFloor(Integer duration, List<PathRes> path) {
            this.duration = duration;
            this.path = path;
        }
    }

    // 같은 건물, 같은 층에서의 최단 경로
    private RouteOnFloor getRouteOnFloor(Building building, int floor, Node startNode, Node endNode) {
        // 출발지와 목적지가 같은 경우
        if(startNode.getId().equals(endNode.getId())) {
            return new RouteOnFloor(0, List.of(new PathRes(startNode)));
        }

        // 최단 경로 찾기
        Map<Long, Integer> distances = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistance> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::getDistance));
        List<Node> nodes = nodeRepository.findByBuildingAndFloorAndRouting(building, floor, true);
        nodes.add(startNode);
        nodes.add(endNode);

        // 초기화
        for (Node node : nodes) {
            Long nodeId = node.getId();
            distances.put(nodeId, nodeId.equals(startNode.getId()) ? 0 : INF_DURATION);
            previousNodes.put(nodeId, null);
            priorityQueue.add(new NodeDistance(nodeId, distances.get(nodeId)));
        }

        while (!priorityQueue.isEmpty()) {
            Long currentNodeId = priorityQueue.poll().getNodeId();
            Map<Long, Integer> neighbors = findNeighbors(currentNodeId, endNode.getId());

            if (!neighbors.isEmpty()) {
                for (Long neighborId : neighbors.keySet()) {
                    int newDistance = distances.get(currentNodeId) + neighbors.get(neighborId);
                    log.info("neighborId: {}", neighborId);
                    if (newDistance < distances.get(neighborId)) {
                        distances.put(neighborId, newDistance);
                        previousNodes.put(neighborId, currentNodeId);
                        priorityQueue.add(new NodeDistance(neighborId, newDistance));
                    }
                }
            }
        }

        // 최단 경로 계산
        List<PathRes> shortestPath = new ArrayList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            shortestPath.add(new PathRes(currentNode));
            if(previousNodes.get(currentNode.getId()) == null) {
                break;
            }
            currentNode = findNode(previousNodes.get(currentNode.getId()));
        }
        Collections.reverse(shortestPath);

        // 결과 출력
        if(!Objects.equals(shortestPath.get(0).getNodeId(), startNode.getId())) {
            log.info("경로를 찾을 수 없습니다.");
            return new RouteOnFloor(INF_DURATION, Collections.emptyList());
        }
        return new RouteOnFloor(distances.get(endNode.getId()), shortestPath);
    }

    private Map<Long, Integer> findNeighbors(Long nodeId, Long endNodeId) {
        Map<Long, Integer> neighbors = new HashMap<>();
        Node node = nodeRepository.findById(nodeId).orElseThrow();
        List<Edge> edges = edgeRepository.findByStartNode(node);
        for(Edge edge : edges) {
            Node edgeEndNode = edge.getEndNode();
            if(edgeEndNode.isRouting() || edgeEndNode.getId().equals(endNodeId)) neighbors.put(edge.getEndNode().getId(), edge.getDistance());
        }
        return neighbors;
    }

    @Getter
    public static class NodeDistance {
        private final Long nodeId;
        private final int distance;

        public NodeDistance(Long nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    // 외부 경로 찾기
    private OuterRouteRes getOuterRoute(BuildingEntrance startBuildingEntrance, BuildingEntrance endBuildingEntrance)
        throws ParseException {

        //네이버 도보 길찾기 결과 문자열로 불러오기
        String url = String.format("https://map.naver.com/p/api/directions/walk?o=reco,wide,flat&l=%f,%f;%f,%f",
            startBuildingEntrance.getLongitude(), startBuildingEntrance.getLatitude(), endBuildingEntrance.getLongitude(), endBuildingEntrance.getLatitude());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        String JsonInString = restTemplate.getForObject(url, String.class);

        JSONParser jsonParser = new JSONParser();
        Object object = jsonParser.parse(JsonInString);
        JSONObject jsonObject = (JSONObject) object;
        JSONArray arr = (JSONArray) jsonObject.get("routes");
        JSONObject mainBody = (JSONObject) arr.get(0);

        //JSON 파싱해서 소요시간 가져오기
        JSONObject summaryBody = (JSONObject) mainBody.get("summary");
        Integer duration = (Integer) summaryBody.get("duration");

        //JSON 파싱해서 경로 가져오기
        JSONArray routeBody = (JSONArray) mainBody.get("legs");
        JSONObject route = (JSONObject) routeBody.get(0);
        JSONArray steps = (JSONArray) route.get("steps");
        List<Double[]> routeList = new ArrayList<>();
        int LastIndex = -1;
        for (int i = 0; i<steps.size(); i++){
            JSONObject pathInfo = (JSONObject) steps.get(i);
            if (pathInfo.get("path") != null){
                String unparsedPath = (String) pathInfo.get("path");
                String[] parsedPath = unparsedPath.split(" ");
                for (int j = 0; j<parsedPath.length; j++){
                    if ((LastIndex == -1) ||(!Objects.equals(routeList.get(LastIndex)[1]+","+routeList.get(LastIndex)[0],parsedPath[j]))) {
                        String[] splitPath = parsedPath[j].split(",");
                        Double[] returnPath = {Double.parseDouble(splitPath[1]),Double.parseDouble(splitPath[0])};
                        routeList.add(returnPath);
                        LastIndex++;
                    }
                }
            }
        }
        return new OuterRouteRes(startBuildingEntrance, endBuildingEntrance, duration, routeList);
    }

    // findEntity 메서드
    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
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

    // 가까운 출입구 찾기
    private BuildingEntrance findNearestEntrance(Building building, Building target) {
        double minDistance = -1;
        BuildingEntrance nearestEntrance = null;
        List<BuildingEntrance> entranceList = buildingEntranceRepository.findAllByBuilding(building);
        for(BuildingEntrance entrance : entranceList) {
            double distance = getEuclidDistance(entrance.getLatitude(), entrance.getLongitude(), target.getLatitude(), target.getLongitude());
            if(minDistance == -1 || minDistance > distance) {
                nearestEntrance = entrance;
                minDistance = distance;
            }
        }
        if(nearestEntrance == null) throw new GlobalException(NOT_FOUND_ENTRANCE);

        return nearestEntrance;
    }

    private double getEuclidDistance(double startX, double startY, double endX, double endY) {
        return Math.pow((startX - endX), 2) + Math.pow((startY - endY), 2);
    }
}
