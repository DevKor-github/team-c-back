package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateView;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrowdCandidateSelectorTest {
    @Mock LlmGateway gateway;

    @Test
    void returnsProviderSelectedIndexWithoutExposingAnId() {
        when(gateway.selectCrowdCandidate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq("커피 파는 데"),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new CrowdCandidateSelection(CrowdCandidateSelection.Status.SELECTED, 2));

        CrowdCandidateSelection result = new CrowdCandidateSelector(gateway).select("커피 파는 데", List.of(
                new CrowdCandidateView(0, "B1층 라운지", "-1", "LOUNGE"),
                new CrowdCandidateView(1, "3층 라운지", "3", "LOUNGE"),
                new CrowdCandidateView(2, "블루포트", "3", "CAFE")));

        assertThat(result.status()).isEqualTo(CrowdCandidateSelection.Status.SELECTED);
        assertThat(result.candidateIndex()).isEqualTo(2);
    }

    @Test
    void neverLetsBareConfirmationSelectTheFirstPlace() {
        CrowdCandidateSelection result = new CrowdCandidateSelector(gateway).select("응", List.of(
                new CrowdCandidateView(0, "B1층 라운지", "-1", "LOUNGE"),
                new CrowdCandidateView(1, "블루포트", "3", "CAFE")));

        assertThat(result.status()).isEqualTo(CrowdCandidateSelection.Status.AMBIGUOUS);
        assertThat(result.candidateIndex()).isNull();
        verifyNoInteractions(gateway);
    }
}
