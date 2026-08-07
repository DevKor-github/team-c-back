package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.response.UpdateNoticeListRes;
import devkor.com.teamcback.domain.notification.dto.response.UpdateNoticeRes;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import devkor.com.teamcback.domain.notification.repository.UpdateNoticeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateNoticeService {

    private final UpdateNoticeRepository updateNoticeRepository;
    private final VersionService versionService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UpdateNoticeListRes getPublishedNotices() {
        var notices = updateNoticeRepository
                .findAllByStatusAndPublishedAtLessThanEqualOrderByPublishedAtDescUpdateNoticeIdDesc(
                        UpdateNoticeStatus.PUBLISHED,
                        LocalDateTime.now(clock)
                )
                .stream()
                .map(UpdateNoticeRes::new)
                .toList();

        return new UpdateNoticeListRes(versionService.getVersion(), notices);
    }
}
