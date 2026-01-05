package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.koyeon.entity.Koyeon;
import devkor.com.teamcback.domain.notification.entity.Version;
import devkor.com.teamcback.domain.notification.repository.VersionRepository;
import devkor.com.teamcback.domain.schoolcalendar.entity.SchoolCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;

    /**
     * 고연전 여부 확인
     */
    @Transactional(readOnly = true)
    public String getVersion() {
        Version version = versionRepository.findById(1L).orElse(null);

        if (version == null) return "";
        return version.getVersion();
    }
}
