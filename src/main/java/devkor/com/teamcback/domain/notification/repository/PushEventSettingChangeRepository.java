package devkor.com.teamcback.domain.notification.repository;
import devkor.com.teamcback.domain.notification.entity.PushEventSettingChange;
import devkor.com.teamcback.domain.notification.persistence.PushStore;
import org.springframework.data.jpa.repository.JpaRepository;
@PushStore
public interface PushEventSettingChangeRepository extends JpaRepository<PushEventSettingChange,Long> {}
