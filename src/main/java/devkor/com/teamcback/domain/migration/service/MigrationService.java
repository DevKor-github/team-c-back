package devkor.com.teamcback.domain.migration.service;

import devkor.com.teamcback.domain.migration.entity.Migration;
import devkor.com.teamcback.domain.migration.repository.MigrationRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_MIGRATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {
    private final MigrationRepository migrationRepository;

    /**
     * 스토어 이전 공지 필요 여부 확인
     */
    @Transactional(readOnly = true)
    public Migration isMigrating() {
        return migrationRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_MIGRATION));
    }
}
