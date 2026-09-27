package devkor.com.teamcback.domain.chatbot.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotCampusSearchServiceTest {
    @Mock
    private BuildingNicknameRepository buildingNicknameRepository;
    @Mock
    private PlaceNicknameRepository placeNicknameRepository;

    private ChatbotCampusSearchService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotCampusSearchService(buildingNicknameRepository, placeNicknameRepository);
    }

    @Test
    void resolvesBuildingAndPlaceWithoutLoadingNodeOrWholeBuildingPlaces() {
        when(buildingNicknameRepository.findChatbotByJaso(any(), any()))
                .thenReturn(List.of(new ChatbotBuildingCandidate(1L, "Main Building", "main")));
        when(placeNicknameRepository.findChatbotByJaso(any(), any()))
                .thenReturn(List.of(new ChatbotPlaceCandidate(2L, "Room 129B", 1L,
                        "Main Building", 1.0, PlaceType.CLASSROOM, "detail", "room")));

        List<ChatbotSearchCandidate> result = service.search("main");

        assertThat(result).extracting(ChatbotSearchCandidate::locationId)
                .containsExactly(1L, 2L);
        verify(buildingNicknameRepository).findChatbotByJaso(any(), any());
        verify(placeNicknameRepository).findChatbotByJaso(any(), any());
    }

    @Test
    void compositeSearchUsesBuildingIdsForPlaceProjection() {
        lenient().when(buildingNicknameRepository.findChatbotByJaso(any(), any())).thenReturn(List.of());
        when(buildingNicknameRepository.findChatbotByJaso(eq("building"), any()))
                .thenReturn(List.of(new ChatbotBuildingCandidate(7L, "Building", "building")));
        when(buildingNicknameRepository.findChatbotByJaso(eq("room"), any()))
                .thenReturn(List.of());
        when(placeNicknameRepository.findChatbotByJasoAndBuildingIds(eq("room"), eq(List.of(7L)), any()))
                .thenReturn(List.of(new ChatbotPlaceCandidate(8L, "Room", 7L,
                        "Building", 1.0, PlaceType.CLASSROOM, null, "room")));

        List<ChatbotSearchCandidate> result = service.search("building room");

        assertThat(result).extracting(ChatbotSearchCandidate::locationId)
                .containsExactly(7L, 8L);
        verify(placeNicknameRepository).findChatbotByJasoAndBuildingIds(eq("room"), eq(List.of(7L)), any());
    }

    @Test
    void deduplicatesSameLocationAndKeepsDistinctIds() {
        when(buildingNicknameRepository.findChatbotByJaso(any(), any())).thenReturn(List.of(
                new ChatbotBuildingCandidate(1L, "Building", "one"),
                new ChatbotBuildingCandidate(1L, "Building", "alias"),
                new ChatbotBuildingCandidate(2L, "Building Annex", "two")));

        List<ChatbotSearchCandidate> result = service.search("building");

        assertThat(result).extracting(ChatbotSearchCandidate::locationId)
                .containsExactly(1L, 2L);
    }
}
