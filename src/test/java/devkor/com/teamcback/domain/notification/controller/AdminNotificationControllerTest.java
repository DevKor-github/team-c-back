package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.response.AdminPushEventFlagRes;
import devkor.com.teamcback.domain.notification.entity.type.PushEventType;
import devkor.com.teamcback.domain.notification.service.AdminNotificationService;
import devkor.com.teamcback.domain.notification.service.PushEventFlagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private PushEventFlagService pushEventFlagService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminNotificationController controller = new AdminNotificationController(
                adminNotificationService,
                pushEventFlagService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void updatedEventFlagIsReflectedInAdminApiQueryResult() throws Exception {
        when(pushEventFlagService.updateFlag(PushEventType.REPORT, true))
                .thenReturn(new AdminPushEventFlagRes(PushEventType.REPORT, true));
        when(pushEventFlagService.getFlags())
                .thenReturn(List.of(
                        new AdminPushEventFlagRes(PushEventType.CROWD, false),
                        new AdminPushEventFlagRes(PushEventType.REPORT, true),
                        new AdminPushEventFlagRes(PushEventType.CHARACTER, false)
                ));

        mockMvc.perform(patch("/api/admin/notifications/event-flags/REPORT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventType").value("REPORT"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(get("/api/admin/notifications/event-flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].eventType").value("REPORT"))
                .andExpect(jsonPath("$.data[1].enabled").value(true));
    }
}
