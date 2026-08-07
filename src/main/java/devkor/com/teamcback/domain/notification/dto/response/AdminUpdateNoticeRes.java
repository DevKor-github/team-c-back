package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUpdateNoticeRes(
        Long id,
        String title,
        String description,
        List<String> features,
        LocalDateTime publishedAt,
        String appVersion,
        boolean show,
        String linkUrl,
        String linkLabel,
        UpdateNoticeStatus status,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime modifiedAt,
        Long modifiedBy
) {
    public AdminUpdateNoticeRes(UpdateNotice notice) {
        this(
                notice.getUpdateNoticeId(),
                notice.getTitle(),
                notice.getDescription(),
                List.copyOf(notice.getFeatures()),
                notice.getPublishedAt(),
                notice.getAppVersion(),
                notice.isShow(),
                notice.getLinkUrl(),
                notice.getLinkLabel(),
                notice.getStatus(),
                notice.getCreatedAt(),
                notice.getCreatedBy(),
                notice.getModifiedAt(),
                notice.getModifiedBy()
        );
    }
}
