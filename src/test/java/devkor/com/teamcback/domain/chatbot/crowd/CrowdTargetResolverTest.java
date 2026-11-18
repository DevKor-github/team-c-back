package devkor.com.teamcback.domain.chatbot.crowd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrowdTargetResolverTest {
    @Mock BLEDeviceRepository repository;

    @Test
    void matchesDeviceNameToTheActualPlaceId() {
        when(repository.findAllChatbotCrowdPlaces()).thenReturn(List.of(
                candidate(4429L, "SK미래관 3층 라운지", 33L),
                candidate(4443L, "SK미래관 블루포트", 33L)));

        CrowdTargetResolver.Resolution lounge = new CrowdTargetResolver(repository)
                .resolve("SK미래관 3층 라운지", List.of());
        CrowdTargetResolver.Resolution cafe = new CrowdTargetResolver(repository)
                .resolve("SK미래관 블루포트", List.of());

        assertThat(lounge.status()).isEqualTo(CrowdTargetResolver.Resolution.Status.UNIQUE);
        assertThat(lounge.candidates()).extracting(CrowdPlaceCandidate::placeId).containsExactly(4429L);
        assertThat(cafe.candidates()).extracting(CrowdPlaceCandidate::placeId).containsExactly(4443L);
    }

    @Test
    void buildingResolutionReturnsDistinctCrowdPlaces() {
        when(repository.findChatbotCrowdPlacesByBuildingId(33L)).thenReturn(List.of(
                candidate(4385L, "SK미래관 B1층 라운지", 33L),
                candidate(4385L, "SK미래관 B1층 라운지", 33L),
                candidate(4479L, "SK미래관 라운지 517호", 33L)));

        CrowdTargetResolver.Resolution resolution = new CrowdTargetResolver(repository).byBuilding(33L);

        assertThat(resolution.status()).isEqualTo(CrowdTargetResolver.Resolution.Status.AMBIGUOUS);
        assertThat(resolution.candidates()).extracting(CrowdPlaceCandidate::placeId)
                .containsExactly(4385L, 4479L);
    }

    @Test
    void resolvesSelectionsWithinBuildingFromCanonicalCrowdNames() {
        when(repository.findChatbotCrowdPlacesByBuildingId(33L)).thenReturn(List.of(
                candidate(4385L, "SK\uBBF8\uB798\uAD00 B1\uCE35 \uB77C\uC6B4\uC9C0", 33L),
                candidate(4429L, "SK\uBBF8\uB798\uAD00 3\uCE35 \uB77C\uC6B4\uC9C0", 33L),
                candidate(4443L, "SK\uBBF8\uB798\uAD00 \uBE14\uB8E8\uD3EC\uD2B8", 33L),
                candidate(4479L, "SK\uBBF8\uB798\uAD00 \uB77C\uC6B4\uC9C0 517\uD638", 33L)));

        CrowdTargetResolver resolver = new CrowdTargetResolver(repository);

        assertThat(resolver.resolveWithinBuilding("\uBE14\uB8E8\uD3EC\uD2B8", 33L).status())
                .isEqualTo(CrowdTargetResolver.Resolution.Status.UNIQUE);
        assertThat(resolver.resolveWithinBuilding("\uBE14\uB8E8\uD3EC\uD2B8", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4443L);
        assertThat(resolver.resolveWithinBuilding("SK\uBBF8\uB798\uAD00 \uBE14\uB8E8\uD3EC\uD2B8", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4443L);
        assertThat(resolver.resolveWithinBuilding("B1\uCE35 \uB77C\uC6B4\uC9C0", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4385L);
        assertThat(resolver.resolveWithinBuilding("SK\uBBF8\uB798\uAD00 B1\uCE35 \uB77C\uC6B4\uC9C0", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4385L);
        assertThat(resolver.resolveWithinBuilding("3\uCE35 \uB77C\uC6B4\uC9C0", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4429L);
        assertThat(resolver.resolveWithinBuilding("517\uD638 \uB77C\uC6B4\uC9C0", 33L).candidates())
                .extracting(CrowdPlaceCandidate::placeId).containsExactly(4479L);
    }

    private CrowdPlaceCandidate candidate(Long placeId, String deviceName, Long buildingId) {
        return new CrowdPlaceCandidate(placeId, deviceName, "라운지", 1D, null, buildingId, "SK미래관");
    }
}
