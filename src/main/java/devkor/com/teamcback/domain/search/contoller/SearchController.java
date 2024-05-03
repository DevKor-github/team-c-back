package devkor.com.teamcback.domain.search.contoller;

import devkor.com.teamcback.domain.search.dto.response.AutoCompleteRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    @GetMapping( )
    public List<AutoCompleteRes> autoComplete(@RequestParam(name = "building_id", required = false) Long buildingId,
        @RequestParam(name = "word") String word) {
        return searchService.autoComplete(buildingId, word);
    }

}
