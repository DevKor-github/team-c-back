package devkor.com.teamcback.domain.notification.repository;
import devkor.com.teamcback.domain.notification.entity.PushEventSetting;
import devkor.com.teamcback.domain.notification.persistence.PushStore;
import org.springframework.data.jpa.repository.JpaRepository;
@PushStore
public interface PushEventSettingRepository extends JpaRepository<PushEventSetting,String> {}
