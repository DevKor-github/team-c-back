package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushMessageRepository extends JpaRepository<PushMessage, Long> {

    List<PushMessage> findAllByDispatch(
            PushDispatch dispatch
    );
}
