package devkor.com.teamcback.domain.notification.repository;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import java.util.*;
import org.springframework.data.domain.Pageable;

public interface PushInstallationLookup {
    List<PushInstallationRepository.UserProfile> findUserProfiles(Collection<Long> userIds);
    Set<Long> findAdminUserIds(Collection<Long> userIds);
    List<PushInstallation> searchByUserProfile(String pattern, AppVariant variant, boolean adminOnly, Pageable pageable);
}
