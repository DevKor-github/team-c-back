package devkor.com.teamcback.domain.routes.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 앱 시작 시 모든 건물의 nodeMapCache를 미리 채워
 * 첫 요청 시 동시 캐시 미스(스탬피드)로 인한 메모리 폭증을 방지한다.
 *
 * <p>Caffeine의 Cache.get(key, fn)은 동일 키에 대한 동시 계산을 막지 않으므로,
 * 30명이 동시에 처음 요청을 보내면 30개의 스레드가 각자 30K 노드를 DB에서 로드해
 * 한 순간에 270MB+ 의 중복 객체를 생성한다.
 * Startup 워밍업으로 이 문제를 원천 차단한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteGraphCacheWarmup {

    private final BuildingRepository buildingRepository;
    private final RouteGraphCache routeGraphCache;

    /** 가장 많이 사용되는 기본 NodeType 조합 (일반 경로 요청의 기본값) */
    private static final List<NodeType> DEFAULT_NODE_TYPES = Arrays.asList(
        NodeType.NORMAL, NodeType.STAIR, NodeType.ELEVATOR,
        NodeType.ENTRANCE, NodeType.CHECKPOINT
    );

    /**
     * ApplicationReadyEvent: 모든 빈과 DB 연결이 준비된 후 실행.
     * @Transactional readOnly로 Hibernate 세션을 열어 지연 로딩 문제 방지.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void warmUp() {
        List<Building> buildings = buildingRepository.findAll();
        int count = 0;
        for (Building building : buildings) {
            try {
                routeGraphCache.getNodeMap(building, DEFAULT_NODE_TYPES);
                count++;
            } catch (Exception e) {
                log.warn("RouteGraphCache warmup failed for building {}: {}", building.getId(), e.getMessage());
            }
        }
        log.info("RouteGraphCache pre-warmed: {}/{} buildings loaded", count, buildings.size());
    }
}
