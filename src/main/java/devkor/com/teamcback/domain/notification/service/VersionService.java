package devkor.com.teamcback.domain.notification.service;

import static devkor.com.teamcback.domain.notification.config.AppVersionConfig.LATEST_APP_VERSION;

import org.springframework.stereotype.Service;

@Service
public class VersionService {

    public String getVersion() {
        return LATEST_APP_VERSION;
    }
}
