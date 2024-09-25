package devkor.com.teamcback.domain.routes.controller;

import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.entity.LocationType;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routeService;

    /***
     * 시작과 끝 정보를 param으로 받아 경로 리턴
     * @param startType 출발 정보 타입(COORD, BUILDING, PLACE, NODE)
     * @param startId (startType이 COORD가 아닌 경우) 출발 시설 id
     * @param startLat (startType이 COORD인 경우) 출발 위도
     * @param startLong (startType이 COORD인 경우) 출발 경도
     * @param endType 도착 정보 타입(COORD, BUILDING, PLACE, NODE)
     * @param endId (endType이 COORD가 아닌 경우) 도착 시설 id
     * @param endLat (endType이 COORD인 경우) 도착 위도
     * @param endLong (endType이 COORD인 경우) 도착 경도
     *
     */
    @GetMapping()
    @Operation(summary = "출발정보와 도착정보 통한 경로 탐색",
    description = "startType와 endType으로 타입 명시 후 id 또는 위도경도 정보 이용하여 경로 탐색. barrierFree를 통해 사용하지 않을 이동수단(계단 혹은 엘리베이터) 명시.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "Not Found",
        content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<List<GetRouteRes>> findRoute(
        @Parameter(name = "startType", description = "출발 장소의 LocationType", example = "PLACE", required = true)
        @RequestParam LocationType startType,
        @Parameter(name = "startId", description = "startType이 COORD가 아닐 경우 해당 시설의 ID")
        @RequestParam(required = false) Long startId,
        @Parameter(name = "startLat", description = "startType이 COORD일 경우 해당 장소의 위도")
        @RequestParam(required = false) Double startLat,
        @Parameter(name = "startLong", description = "startType이 COORD일 경우 해당 장소의 경도")
        @RequestParam(required = false) Double startLong,
        @Parameter(name = "endType", description = "도착 장소의 LocationType", example = "BUILDING", required = true)
        @RequestParam LocationType endType,
        @Parameter(name = "endId", description = "endType이 COORD가 아닐 경우 해당 시설의 ID")
        @RequestParam(required = false) Long endId,
        @Parameter(name = "endLat", description = "endType이 COORD일 경우 해당 장소의 위도")
        @RequestParam(required = false) Double endLat,
        @Parameter(name = "endLong", description = "endType이 COORD일 경우 해당 장소의 경도")
        @RequestParam(required = false) Double endLong) throws ParseException {
        List<Double> startLocation = new ArrayList<>();
        List<Double> endLocation = new ArrayList<>();

        if (startType == LocationType.COORD){
            startLocation.addAll(Arrays.asList(startLat, startLong));
        }
        else{
            startLocation.add(startId.doubleValue());
        }
        if (endType == LocationType.COORD){
            endLocation.addAll(Arrays.asList(endLat, endLong));
        }
        else{
            endLocation.add(endId.doubleValue());
        }
        List<GetRouteRes> returnList = new ArrayList<>();
        returnList.add(routeService.findRoute(startLocation, startType, endLocation, endType, NodeType.SHUTTLE));
        return CommonResponse.success(returnList);
    }
}
