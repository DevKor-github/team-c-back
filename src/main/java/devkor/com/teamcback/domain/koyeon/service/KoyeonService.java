package devkor.com.teamcback.domain.koyeon.service;

import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubInfoRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubListRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubRes;
import devkor.com.teamcback.domain.koyeon.entity.*;
import devkor.com.teamcback.domain.koyeon.repository.*;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 고연전 여부 확인
     */
    @Transactional(readOnly = true)
    public Koyeon isKoyeon() {
        return koyeonRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_KOYEON));
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

    private FreePub findFreePub(Long pubId) {
        return freePubRepository.findById(pubId).orElseThrow(() -> new GlobalException(NOT_FOUND_PUB));
    }
}
