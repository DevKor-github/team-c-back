package devkor.com.teamcback.domain.notification.service;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

import devkor.com.teamcback.domain.notification.entity.Version;
import devkor.com.teamcback.domain.notification.repository.VersionRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VersionService {

    private static final long REQUIRED_VERSION_SETTING_ID = 1L;
    private static final Pattern APP_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private final VersionRepository versionRepository;

    /**
     * 구버전 앱의 문자열 응답 계약을 유지한다. 이 값은 최신 출시 버전이 아니라
     * 이 버전 미만의 앱을 차단하는 최소 지원 버전이다.
     */
    @Transactional(readOnly = true)
    public String getMinimumRequiredVersion() {
        return versionRepository.findById(REQUIRED_VERSION_SETTING_ID)
                .map(Version::getVersion)
                .orElse("");
    }

    @Transactional
    public String updateMinimumRequiredVersion(String minimumRequiredVersion) {
        String normalizedVersion = minimumRequiredVersion == null
                ? ""
                : minimumRequiredVersion.trim();
        if (!StringUtils.hasText(normalizedVersion)
                || !APP_VERSION_PATTERN.matcher(normalizedVersion).matches()) {
            throw new GlobalException(INVALID_INPUT);
        }

        Version version = versionRepository.findById(REQUIRED_VERSION_SETTING_ID)
                .orElseGet(() -> new Version(normalizedVersion));
        version.update(normalizedVersion);
        return versionRepository.save(version).getVersion();
    }
}
