package devkor.com.teamcback.domain.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import devkor.com.teamcback.domain.notification.dto.response.UpdateNoticeListRes;
import devkor.com.teamcback.domain.notification.dto.response.UpdateNoticeRes;
import devkor.com.teamcback.domain.notification.service.UpdateNoticeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UpdateNoticeControllerTest {

    @Mock
    private UpdateNoticeService updateNoticeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UpdateNoticeController(updateNoticeService)
        ).build();
    }

    @Test
    void returnsStructuredNoticeContract() throws Exception {
        when(updateNoticeService.getPublishedNotices()).thenReturn(
                new UpdateNoticeListRes(
                        "2.3.0",
                        List.of(new UpdateNoticeRes(
                                17L,
                                "포인트 상점 업데이트",
                                "연결을 개선했어요.",
                                List.of("상점 연결 안정화"),
                                LocalDateTime.of(2026, 8, 7, 10, 30),
                                "2.3.0",
                                true,
                                "https://example.com/notice",
                                "공지 원문 보기"
                        ))
                )
        );

        mockMvc.perform(get("/api/notifications/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data.latestVersion").value("2.3.0"))
                .andExpect(jsonPath("$.data.notices[0].id").value(17))
                .andExpect(jsonPath("$.data.notices[0].show").value(true))
                .andExpect(jsonPath("$.data.notices[0].linkUrl").value("https://example.com/notice"))
                .andExpect(jsonPath("$.data.notices[0].features[0]")
                        .value("상점 연결 안정화"));
    }
}
