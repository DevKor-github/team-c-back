package devkor.com.teamcback.domain.koyeon.service;

import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubInfoRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubListRes;
import devkor.com.teamcback.domain.koyeon.dto.response.SearchFreePubRes;
import devkor.com.teamcback.domain.koyeon.entity.Koyeon;
import devkor.com.teamcback.domain.koyeon.repository.FreePubRepository;
import devkor.com.teamcback.domain.koyeon.repository.KoyeonRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_KOYEON;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PUB;

@Slf4j
@Service
@RequiredArgsConstructor
public class KoyeonService {
    private final KoyeonRepository koyeonRepository;
    private final FreePubRepository freePubRepository;

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
    public SearchFreePubListRes searchFreePubList() {
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
        return new SearchFreePubInfoRes(freePubRepository.findById(pubId).orElseThrow(() -> new GlobalException(NOT_FOUND_PUB)));
    }


}
