package devkor.com.teamcback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import devkor.com.teamcback.domain.notification.repository.UpdateNoticeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateNoticeServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-07T03:00:00Z"),
            SEOUL
    );

    @Mock
    private UpdateNoticeRepository updateNoticeRepository;
    @Mock
    private VersionService versionService;

    private UpdateNoticeService service;

    @BeforeEach
    void setUp() {
        service = new UpdateNoticeService(updateNoticeRepository, versionService, CLOCK);
    }

    @Test
    void returnsPublishedNoticeListWithPopupEligibilityAndLatestVersion() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        var popupNotice = new UpdateNotice(
                "포인트 상점 업데이트",
                "포인트 상점 연결을 개선했어요.",
                List.of("상점 연결 안정화", "업데이트 공지 목록 추가"),
                now.minusDays(1),
                "2.3.0",
                true,
                "https://example.com/update",
                "업데이트 원문"
        );
        var listOnlyNotice = new UpdateNotice(
                "이전 업데이트",
                "공지 목록에는 남지만 팝업으로 노출하지 않아요.",
                List.of(),
                now.minusDays(2),
                "2.2.0",
                false
        );

        when(versionService.getVersion()).thenReturn("2.3.0");
        when(updateNoticeRepository
                .findAllByPublishedAtLessThanEqualOrderByPublishedAtDescUpdateNoticeIdDesc(now))
                .thenReturn(List.of(popupNotice, listOnlyNotice));

        var result = service.getPublishedNotices();

        verify(updateNoticeRepository)
                .findAllByPublishedAtLessThanEqualOrderByPublishedAtDescUpdateNoticeIdDesc(now);
        assertThat(result.latestVersion()).isEqualTo("2.3.0");
        assertThat(result.notices()).hasSize(2);
        assertThat(result.notices().get(0).show()).isTrue();
        assertThat(result.notices().get(0).features())
                .containsExactly("상점 연결 안정화", "업데이트 공지 목록 추가");
        assertThat(result.notices().get(0).linkUrl()).isEqualTo("https://example.com/update");
        assertThat(result.notices().get(1).show()).isFalse();
    }
}
