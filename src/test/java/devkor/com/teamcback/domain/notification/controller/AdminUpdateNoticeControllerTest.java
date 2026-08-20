package devkor.com.teamcback.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import devkor.com.teamcback.domain.notification.dto.response.AdminUpdateNoticeRes;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import devkor.com.teamcback.domain.notification.service.AdminUpdateNoticeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUpdateNoticeControllerTest {

    @Mock
    private AdminUpdateNoticeService adminUpdateNoticeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminUpdateNoticeController(adminUpdateNoticeService)
        ).build();
    }

    @Test
    void createsNoticeThroughNoticeOnlyEndpoint() throws Exception {
        when(adminUpdateNoticeService.createNotice(any())).thenReturn(new AdminUpdateNoticeRes(
                17L,
                "고대로 1.1.6 업데이트",
                "새 기능을 안내합니다.",
                List.of("공지 관리자 추가"),
                LocalDateTime.of(2026, 8, 8, 10, 0),
                "1.1.6",
                true,
                null,
                null,
                UpdateNoticeStatus.PUBLISHED,
                null,
                null,
                null,
                null
        ));

        mockMvc.perform(post("/api/admin/notifications/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "고대로 1.1.6 업데이트",
                                  "description": "새 기능을 안내합니다.",
                                  "features": ["공지 관리자 추가"],
                                  "publishedAt": "2026-08-08T10:00:00",
                                  "appVersion": "1.1.6",
                                  "show": true,
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data.id").value(17))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.show").value(true));
    }
}
