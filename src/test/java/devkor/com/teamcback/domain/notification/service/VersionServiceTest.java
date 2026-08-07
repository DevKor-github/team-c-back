package devkor.com.teamcback.domain.notification.service;

import static devkor.com.teamcback.domain.notification.config.AppVersionConfig.LATEST_APP_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionServiceTest {

    private final VersionService versionService = new VersionService();

    @Test
    void returnsTheSourceControlledAppVersion() {
        assertThat(versionService.getVersion()).isEqualTo(LATEST_APP_VERSION);
    }
}
