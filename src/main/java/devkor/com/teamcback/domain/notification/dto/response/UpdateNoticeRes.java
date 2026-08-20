package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateNoticeRes(
        Long id,
        String title,
        String description,
        List<String> features,
        LocalDateTime publishedAt,
        String appVersion,
        boolean show,
        String linkUrl,
        String linkLabel
) {
    public UpdateNoticeRes(UpdateNotice notice) {
        this(
                notice.getUpdateNoticeId(),
                notice.getTitle(),
                notice.getDescription(),
                List.copyOf(notice.getFeatures()),
                notice.getPublishedAt(),
                notice.getAppVersion(),
                notice.isShow(),
                notice.getLinkUrl(),
                notice.getLinkLabel()
        );
    }
}
