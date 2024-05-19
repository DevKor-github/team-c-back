package devkor.com.teamcback.domain.navigate.controller;

import devkor.com.teamcback.domain.navigate.dto.OuterRouteResponseDto;
import devkor.com.teamcback.domain.navigate.service.OuterRouteService;
import lombok.RequiredArgsConstructor;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//길찾기가 완성되지 않았으므로 임시로 직접 request를 받는 방식으로 제작
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class OuterRouteController {
    private final OuterRouteService outerRouteService;

    @GetMapping()
    public OuterRouteResponseDto getOuterRoute(
        @RequestParam String stLong,
        @RequestParam String stLat,
        @RequestParam String edLong,
        @RequestParam String edLat
    ) throws ParseException {
        return outerRouteService.getOuterRoute(stLong, stLat, edLong, edLat);
    }
}
