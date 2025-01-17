package devkor.com.teamcback.integration;

import devkor.com.teamcback.BaseMvcTest;
import devkor.com.teamcback.domain.routes.entity.LocationType;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.global.exception.AdminException;
import devkor.com.teamcback.global.exception.GlobalException;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class RouteServiceIntegrationTest extends BaseMvcTest {
    @Autowired
    RouteService routeService;

    @Autowired
    NodeRepository nodeRepository;

    @Test
    @Order(1)
    @DisplayName("기본 길찾기 테스트: 노드 - 노드")
    void nodeRouteTest() {
        List<Node> nodeList = nodeRepository.findAll();
        for(Node start : nodeList) {
            for (Node end : nodeList) {
                try {
                    System.out.println("start_node_id: " + start.getId() + " end_node_id: " + end.getId());
                    routeService.findRoute(LocationType.NODE, start.getId(), null, null, LocationType.NODE, end.getId(), null, null, null);
                } catch (AdminException e) {
                    System.out.println(e.getAdminMessage());
                } catch (GlobalException e) {
                    System.out.println(e.getResultCode());
                }
            }
        }
    }

}
