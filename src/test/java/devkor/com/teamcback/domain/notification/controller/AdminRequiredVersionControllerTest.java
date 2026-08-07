package devkor.com.teamcback.domain.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import devkor.com.teamcback.domain.notification.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminRequiredVersionControllerTest {

    @Mock
    private VersionService versionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminRequiredVersionController(versionService)
        ).build();
    }

    @Test
    void getsTheConfiguredMinimumRequiredVersion() throws Exception {
        when(versionService.getMinimumRequiredVersion()).thenReturn("1.1.5");

        mockMvc.perform(get("/api/admin/notifications/required-version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data.minimumRequiredVersion").value("1.1.5"));
    }

    @Test
    void updatesTheMinimumRequiredVersion() throws Exception {
        when(versionService.updateMinimumRequiredVersion("1.1.6")).thenReturn("1.1.6");

        mockMvc.perform(put("/api/admin/notifications/required-version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minimumRequiredVersion":"1.1.6"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minimumRequiredVersion").value("1.1.6"));
    }
}
