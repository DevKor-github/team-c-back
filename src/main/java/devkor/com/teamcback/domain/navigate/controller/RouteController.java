package devkor.com.teamcback.domain.navigate.controller;

import devkor.com.teamcback.domain.navigate.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.navigate.service.RouteService;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routeService;

    @GetMapping()
    public CommonResponse<GetRouteRes> findRoute(@RequestParam Long startBuildingId, @RequestParam(required = false) Long startRoomId, @RequestParam(required = false) PlaceType startType,
        @RequestParam Long endBuildingId, @RequestParam(required = false) Long endRoomId, @RequestParam(required = false) PlaceType endType) throws ParseException {
        return CommonResponse.success(routeService.findRoute(startBuildingId, startRoomId, startType, endBuildingId, endRoomId, endType));
    }
}
