package devkor.com.teamcback.integration;

import devkor.com.teamcback.BaseMvcTest;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.dto.response.GetGraphRes;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import devkor.com.teamcback.domain.routes.entity.LocationType;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

//@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class RouteServiceIntegrationTest extends BaseMvcTest {
    @Autowired
    RouteService routeService;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BuildingRepository buildingRepository;
    @Autowired
    PlaceRepository placeRepository;

    @Test
    @Order(1)
    @DisplayName("기본 길찾기 테스트: 노드 - 노드")
    void nodeRouteTest() {
        List<Node> nodeList = nodeRepository.findRandomNodes(); // 무작위 노드 20개
        for(int i = 0; i < nodeList.size() - 1; i++) {
            Node start = nodeList.get(i);
            for (int j = i+1; j < nodeList.size(); j++) {
                Node end = nodeList.get(j);
                try {
                    System.out.println("-------------------------" + "start_node_id: " + start.getId() + " end_node_id: " + end.getId());
                    routeService.findRoute(LocationType.NODE, start.getId(), null, null, LocationType.NODE, end.getId(), null, null, null);
                } catch (AdminException e) {
                    System.out.println("##########################" + e.getAdminMessage());
                } catch (GlobalException e) {
                    System.out.println("##########################" + e.getResultCode());
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("기본 길찾기 테스트: 빌딩 - 빌딩")
    void buildingRouteTest() {
        List<Building> buildingList = buildingRepository.findAll();
        for(int i = 1; i < buildingList.size() - 1; i++) {
            Building start = buildingList.get(i);
            for (int j = i+1; j < buildingList.size(); j++) {
                Building end = buildingList.get(j);
                try {
                    System.out.println("-------------------------" + "start_building_id: " + start.getId() + " end_building_id: " + end.getId());
                    routeService.findRoute(LocationType.BUILDING, start.getId(), null, null, LocationType.BUILDING, end.getId(), null, null, null);
                } catch (AdminException e) {
                    System.out.println("##########################" + e.getAdminMessage());
                } catch (GlobalException e) {
                    System.out.println("##########################" + e.getResultCode());
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("길찾기 테스트: 연속적")
    //설계 방향성: 기본적으로 경로 설정(place기준, building 기준, node 기준 등) / filter설정
    //filter에 맞게 graph만 미리 사전생성해두고 필요한 경로에 따라 merge 진행, 그 그래프들로 test
    void routeTest(){
        List<Place> placeList = placeRepository.findAll();
        List<Building> buildingList = buildingRepository.findAll();
        HashMap<Building, GetGraphRes> graphResMap = new HashMap<>();
        //conditions 수정 원하면 여기 파트 수정
        List<Conditions> conditions = new ArrayList<>();
        for (Building building : buildingList) {
            graphResMap.put(building, getLocalGraph(building, conditions));
        }
        try {
            List<GetRouteRes> routeRes = routeService.findRouteUsingGraph(LocationType.PLACE, 1L, (double) 0, (double) 0, LocationType.PLACE, 111L, (double) 0, (double) 0,  new ArrayList<>(), graphResMap);
            System.out.println(routeRes);
        } catch (AdminException e) {
            System.out.println("##########################" + e.getAdminMessage());
        } catch (GlobalException e) {
            System.out.println("##########################" + e.getResultCode());
        }
    }

    private GetGraphRes getLocalGraph(Building building, List<Conditions> conditions){
        List<Node> random2Nodes = nodeRepository.findRandomNodesByBuilding(building);
        HashSet<Building> buildingList = new HashSet<>();
        buildingList.add(building);
        return routeService.getGraph(buildingList, random2Nodes.get(0), random2Nodes.get(1), conditions);
    }

}
