package devkor.com.teamcback.domain.navigate.service;

import static devkor.com.teamcback.global.response.ResultCode.*;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.EdgeRepository;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;
    private final FacilityRepository facilityRepository;

//    @Transactional(readOnly = true)
//    public GetRouteRes findRoute(Long startBuildingId, Long startRoomId, PlaceType startType,
//        Long endBuildingId, Long endRoomId, PlaceType endType) throws ParseException {
//
//    }

//    // 외부 경로 찾기
//    private OuterRouteRes getOuterRoute(BuildingEntrance startBuildingEntrance, BuildingEntrance endBuildingEntrance)
//        throws ParseException {
//
//        //네이버 도보 길찾기 결과 문자열로 불러오기
//        String url = String.format("https://map.naver.com/p/api/directions/walk?o=reco,wide,flat&l=%f,%f;%f,%f",
//            startBuildingEntrance.getLongitude(), startBuildingEntrance.getLatitude(), endBuildingEntrance.getLongitude(), endBuildingEntrance.getLatitude());
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
//        String JsonInString = restTemplate.getForObject(url, String.class);
//
//        JSONParser jsonParser = new JSONParser();
//        Object object = jsonParser.parse(JsonInString);
//        JSONObject jsonObject = (JSONObject) object;
//        JSONArray arr = (JSONArray) jsonObject.get("routes");
//        JSONObject mainBody = (JSONObject) arr.get(0);
//
//        //JSON 파싱해서 소요시간 가져오기
//        JSONObject summaryBody = (JSONObject) mainBody.get("summary");
//        Integer duration = (Integer) summaryBody.get("duration");
//
//        //JSON 파싱해서 경로 가져오기
//        JSONArray routeBody = (JSONArray) mainBody.get("legs");
//        JSONObject route = (JSONObject) routeBody.get(0);
//        JSONArray steps = (JSONArray) route.get("steps");
//        List<Double[]> routeList = new ArrayList<>();
//        int LastIndex = -1;
//        for (int i = 0; i<steps.size(); i++){
//            JSONObject pathInfo = (JSONObject) steps.get(i);
//            if (pathInfo.get("path") != null){
//                String unparsedPath = (String) pathInfo.get("path");
//                String[] parsedPath = unparsedPath.split(" ");
//                for (int j = 0; j<parsedPath.length; j++){
//                    if ((LastIndex == -1) ||(!Objects.equals(routeList.get(LastIndex)[1]+","+routeList.get(LastIndex)[0],parsedPath[j]))) {
//                        String[] splitPath = parsedPath[j].split(",");
//                        Double[] returnPath = {Double.parseDouble(splitPath[1]),Double.parseDouble(splitPath[0])};
//                        routeList.add(returnPath);
//                        LastIndex++;
//                    }
//                }
//            }
//        }
//        return new OuterRouteRes(startBuildingEntrance, endBuildingEntrance, duration, routeList);
//    }

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

    private double getEuclidDistance(double startX, double startY, double endX, double endY) {
        return Math.pow((startX - endX), 2) + Math.pow((startY - endY), 2);
    }
}
