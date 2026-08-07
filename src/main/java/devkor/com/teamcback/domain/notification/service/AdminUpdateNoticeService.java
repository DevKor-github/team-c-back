package devkor.com.teamcback.domain.notification.service;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_UPDATE_NOTICE;

import devkor.com.teamcback.domain.notification.dto.request.AdminUpdateNoticeReq;
import devkor.com.teamcback.domain.notification.dto.response.AdminUpdateNoticeRes;
import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import devkor.com.teamcback.domain.notification.repository.UpdateNoticeRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUpdateNoticeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UpdateNoticeRepository updateNoticeRepository;

    @Transactional(readOnly = true)
    public Page<AdminUpdateNoticeRes> getNotices(
            int page,
            int size,
            UpdateNoticeStatus status,
            String query
    ) {
        int normalizedPage = Math.max(page - 1, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "publishedAt", "updateNoticeId")
        );

        Specification<UpdateNotice> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(query)) {
            String keyword = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, builder) -> builder.or(
                    builder.like(builder.lower(root.<String>get("title")), keyword),
                    builder.like(builder.lower(root.<String>get("description")), keyword),
                    builder.like(builder.lower(root.<String>get("appVersion")), keyword)
            ));
        }

        return updateNoticeRepository.findAll(specification, pageable)
                .map(AdminUpdateNoticeRes::new);
    }

    @Transactional(readOnly = true)
    public AdminUpdateNoticeRes getNotice(Long noticeId) {
        return new AdminUpdateNoticeRes(findNotice(noticeId));
    }

    @Transactional
    public AdminUpdateNoticeRes createNotice(AdminUpdateNoticeReq request) {
        var sanitized = sanitize(request);
        var notice = new UpdateNotice(
                sanitized.title(),
                sanitized.description(),
                sanitized.features(),
                sanitized.publishedAt(),
                sanitized.appVersion(),
                sanitized.show(),
                sanitized.linkUrl(),
                sanitized.linkLabel(),
                sanitized.status()
        );
        return new AdminUpdateNoticeRes(updateNoticeRepository.save(notice));
    }

    @Transactional
    public AdminUpdateNoticeRes updateNotice(Long noticeId, AdminUpdateNoticeReq request) {
        var notice = findNotice(noticeId);
        var sanitized = sanitize(request);
        notice.update(
                sanitized.title(),
                sanitized.description(),
                sanitized.features(),
                sanitized.publishedAt(),
                sanitized.appVersion(),
                sanitized.show(),
                sanitized.linkUrl(),
                sanitized.linkLabel(),
                sanitized.status()
        );
        return new AdminUpdateNoticeRes(notice);
    }

    @Transactional
    public AdminUpdateNoticeRes archiveNotice(Long noticeId) {
        var notice = findNotice(noticeId);
        notice.archive();
        return new AdminUpdateNoticeRes(notice);
    }

    private UpdateNotice findNotice(Long noticeId) {
        return updateNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new GlobalException(NOT_FOUND_UPDATE_NOTICE));
    }

    private AdminUpdateNoticeReq sanitize(AdminUpdateNoticeReq request) {
        if (request == null
                || !StringUtils.hasText(request.title())
                || !StringUtils.hasText(request.description())
                || !StringUtils.hasText(request.appVersion())
                || request.publishedAt() == null
                || request.show() == null
                || request.status() == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        List<String> features = request.features() == null
                ? List.of()
                : request.features().stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toList();
        String linkUrl = trimToNull(request.linkUrl());
        String linkLabel = trimToNull(request.linkLabel());
        if (linkUrl != null && !(linkUrl.startsWith("https://") || linkUrl.startsWith("http://"))) {
            throw new GlobalException(INVALID_INPUT);
        }
        if (linkLabel != null && linkUrl == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        return new AdminUpdateNoticeReq(
                request.title().trim(),
                request.description().trim(),
                features,
                request.publishedAt(),
                request.appVersion().trim(),
                request.status() == UpdateNoticeStatus.ARCHIVED ? false : request.show(),
                linkUrl,
                linkLabel,
                request.status()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
