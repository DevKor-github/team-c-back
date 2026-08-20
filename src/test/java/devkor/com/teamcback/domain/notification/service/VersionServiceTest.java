package devkor.com.teamcback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.notification.entity.Version;
import devkor.com.teamcback.domain.notification.repository.VersionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionRepository versionRepository;

    private VersionService versionService;

    @BeforeEach
    void setUp() {
        versionService = new VersionService(versionRepository);
    }

    @Test
    void returnsTheDatabaseBackedMinimumRequiredVersion() {
        when(versionRepository.findById(1L))
                .thenReturn(Optional.of(new Version("1.1.5")));

        assertThat(versionService.getMinimumRequiredVersion()).isEqualTo("1.1.5");
    }

    @Test
    void returnsEmptyWhenTheVersionHasNotBeenConfigured() {
        when(versionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(versionService.getMinimumRequiredVersion()).isEmpty();
    }

    @Test
    void updatesTheExistingMinimumRequiredVersion() {
        var stored = new Version("1.1.5");
        when(versionRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(versionRepository.save(any(Version.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(versionService.updateMinimumRequiredVersion(" 1.1.6 ")).isEqualTo("1.1.6");

        var captor = ArgumentCaptor.forClass(Version.class);
        verify(versionRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo("1.1.6");
    }
}
