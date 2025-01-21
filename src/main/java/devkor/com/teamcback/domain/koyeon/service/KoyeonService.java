package devkor.com.teamcback.domain.koyeon.service;

import devkor.com.teamcback.domain.koyeon.dto.response.*;
import devkor.com.teamcback.domain.koyeon.entity.*;
import devkor.com.teamcback.domain.koyeon.repository.*;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KoyeonService {
    private final KoyeonRepository koyeonRepository;
    private final FreePubRepository freePubRepository;
    private final FoodTagRepository foodTagRepository;
    private final MenuRepository menuRepository;
    private final TagMenuRepository tagMenuRepository;
    private final FreePubNicknameRepository freePubNicknameRepository;

    /**
     * 고연전 여부 확인
     */
    @Transactional(readOnly = true)
    public Koyeon isKoyeon() {
        return koyeonRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_KOYEON));
    }

    /**
     * 주점 통합 검색
     */
    public GlobalPubSearchListRes globalPubSearch(String keyword) {
        List<GlobalPubSearchRes> list = freePubNicknameRepository.findByNicknameContaining(keyword).stream()
            .map(FreePubNickname::getFreePub)
            .map(GlobalPubSearchRes::new)
            .distinct()
            .toList();
        return new GlobalPubSearchListRes(orderSequence(calculateScoreByIndex(list, keyword)));
    }

    /**
     * 무료 주점 List 반환
     */
    @Transactional(readOnly = true)
    public SearchFreePubListRes searchFreePubList(Long tagId) {
        if(tagId != null) {
            FoodTag tag = foodTagRepository.findById(tagId).orElseThrow(() -> new GlobalException(NOT_FOUND_TAG));

            // 태그에 해당하는 음식 리스트
            List<Menu> menuList = tagMenuRepository.findByFoodTag(tag).stream().map(TagMenu::getMenu).toList();

            // 음식을 가진 음식점 리스트
            List<FreePub> pubList = menuList.stream().map(Menu::getFreePub).distinct().toList();

            List<SearchFreePubRes> pubResList = new ArrayList<>();
            for (FreePub pub : pubList) {
                pubResList.add(new SearchFreePubRes(pub, menuRepository.findByFreePub(pub).stream().filter(
                    menuList::contains).map(Menu::getName).toList()));
            }

            return new SearchFreePubListRes(pubResList);
        }

        return new SearchFreePubListRes(freePubRepository.findAll()
            .stream()
            .map(SearchFreePubRes::new)
            .toList());
    }

    /**
     * 특정 주점 정보 반환
     */
    @Transactional(readOnly = true)
    public SearchFreePubInfoRes searchFreePubInfo(Long pubId) {
        FreePub pub = findFreePub(pubId);
        return new SearchFreePubInfoRes(pub, menuRepository.findByFreePub(pub));
    }

    private Map<GlobalPubSearchRes, Integer> calculateScoreByIndex(List<GlobalPubSearchRes> list, String keyword) {
        Map<GlobalPubSearchRes, Integer> scores = new HashMap<>();

        for (GlobalPubSearchRes res : list) {
            int indexScore = 0;
            int index = res.getName().contains(keyword) ? res.getName().indexOf(keyword.charAt(0)) : -1;

            if (index != -1) indexScore += (100 - index);
            scores.put(res, indexScore);
        }
        return scores;
    }

    private static List<GlobalPubSearchRes> orderSequence(Map<GlobalPubSearchRes, Integer> scores) {
        // 점수를 기준으로 그룹화
        Map<Integer, List<GlobalPubSearchRes>> groupedByScore = scores.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
            ));

        // 각 그룹 내의 리스트를 이름 기준으로 정렬
        Map<Integer, List<GlobalPubSearchRes>> sortedGroupedByScore = groupedByScore.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .sorted(Comparator.comparing(GlobalPubSearchRes::getName))
                    .toList(),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        // 정렬된 그룹을 점수 순으로 재결합
        return sortedGroupedByScore.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .flatMap(entry -> entry.getValue().stream())
            .distinct()
            .toList();
    }

    private FreePub findFreePub(Long pubId) {
        return freePubRepository.findById(pubId).orElseThrow(() -> new GlobalException(NOT_FOUND_PUB));
    }
}
