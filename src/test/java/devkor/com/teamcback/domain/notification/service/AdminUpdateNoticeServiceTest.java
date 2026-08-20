package devkor.com.teamcback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.notification.dto.request.AdminUpdateNoticeReq;
import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import devkor.com.teamcback.domain.notification.repository.UpdateNoticeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUpdateNoticeServiceTest {

    @Mock
    private UpdateNoticeRepository updateNoticeRepository;

    private AdminUpdateNoticeService service;

    @BeforeEach
    void setUp() {
        service = new AdminUpdateNoticeService(updateNoticeRepository);
    }

    @Test
    void createsNoticeWithoutDispatchingPush() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 8, 8, 10, 0);
        var request = new AdminUpdateNoticeReq(
                "  고대로 1.1.6 업데이트  ",
                "  새 기능을 안내합니다.  ",
                List.of("  공지 관리자 추가  ", "앱 모달 미리보기"),
                publishedAt,
                " 1.1.6 ",
                true,
                " https://example.com/notice ",
                " 자세히 보기 ",
                UpdateNoticeStatus.DRAFT
        );
        when(updateNoticeRepository.save(any(UpdateNotice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createNotice(request);

        var captor = ArgumentCaptor.forClass(UpdateNotice.class);
        verify(updateNoticeRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("고대로 1.1.6 업데이트");
        assertThat(captor.getValue().getFeatures())
                .containsExactly("공지 관리자 추가", "앱 모달 미리보기");
        assertThat(result.status()).isEqualTo(UpdateNoticeStatus.DRAFT);
        assertThat(result.show()).isTrue();
    }

    @Test
    void archiveKeepsNoticeAndDisablesPopup() {
        var notice = new UpdateNotice(
                "공지",
                "본문",
                List.of(),
                LocalDateTime.now(),
                "1.1.5",
                true
        );
        when(updateNoticeRepository.findById(7L)).thenReturn(java.util.Optional.of(notice));

        var result = service.archiveNotice(7L);

        assertThat(result.status()).isEqualTo(UpdateNoticeStatus.ARCHIVED);
        assertThat(result.show()).isFalse();
    }
}
