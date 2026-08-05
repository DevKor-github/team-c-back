package devkor.com.teamcback.domain.notification.listener;

import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushMode;
import devkor.com.teamcback.domain.notification.entity.type.PushTargetType;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.service.PushDispatchService;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CrowdVacantPushEventListenerTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PushInstallationRepository pushInstallationRepository;

    @Mock
    private PushDispatchService pushDispatchService;

    private CrowdVacantPushEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CrowdVacantPushEventListener(
                placeRepository,
                categoryRepository,
                pushInstallationRepository,
                pushDispatchService
        );
    }

    @Test
    void createsUserDispatchesForDistinctFavoriteUsers() {
        ReflectionTestUtils.setField(listener, "crowdEnabled", true);
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place("신공학관", "라운지")));
        when(categoryRepository.findDistinctUserIdsByLocationTypeAndLocationId(LocationType.PLACE, 10L))
                .thenReturn(List.of(1L, 1L, 2L));
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(1L, AppVariant.PRODUCTION))
                .thenReturn(true);
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(2L, AppVariant.PRODUCTION))
                .thenReturn(true);

        listener.handle(event());

        ArgumentCaptor<PushDispatchCommand> captor = ArgumentCaptor.forClass(PushDispatchCommand.class);
        verify(pushDispatchService, times(2)).enqueue(captor.capture());

        PushDispatchCommand first = captor.getAllValues().get(0);
        assertThat(first.targetType()).isEqualTo(PushTargetType.USER);
        assertThat(first.targetValue()).isEqualTo("1");
        assertThat(first.mode()).isEqualTo(PushMode.ACTUAL);
        assertThat(first.appVariant()).isEqualTo(AppVariant.PRODUCTION);
        assertThat(first.actionType()).isEqualTo(PushActionType.PLACE_DETAIL);
        assertThat(first.actionParams()).containsEntry("placeId", 10L);
        assertThat(first.title()).isEqualTo("기다리던 자리가 생겼어요!");
        assertThat(first.body()).isEqualTo("신공학관 라운지이 한산해요. 방문하기 전 현황을 확인해보세요.");
        assertThat(first.body()).doesNotContain("null");
        assertThat(first.idempotencyKey()).isEqualTo("crowd-vacant:10:1:99");
    }

    @Test
    void doesNotCreateDispatchWhenNoFavoriteUsersExist() {
        ReflectionTestUtils.setField(listener, "crowdEnabled", true);
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place("신공학관", "라운지")));
        when(categoryRepository.findDistinctUserIdsByLocationTypeAndLocationId(LocationType.PLACE, 10L))
                .thenReturn(List.of());

        listener.handle(event());

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotCreateDispatchWhenFeatureFlagIsFalse() {
        ReflectionTestUtils.setField(listener, "crowdEnabled", false);

        listener.handle(event());

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
        verify(placeRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsUsersWithoutProductionInstallation() {
        ReflectionTestUtils.setField(listener, "crowdEnabled", true);
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place(null, "라운지")));
        when(categoryRepository.findDistinctUserIdsByLocationTypeAndLocationId(LocationType.PLACE, 10L))
                .thenReturn(List.of(1L));
        when(pushInstallationRepository.existsByUserIdAndAppVariantAndActiveTrue(1L, AppVariant.PRODUCTION))
                .thenReturn(false);

        listener.handle(event());

        verify(pushDispatchService, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }

    private PlaceBecameVacantEvent event() {
        return new PlaceBecameVacantEvent(
                10L,
                99L,
                LocalDateTime.parse("2026-08-05T10:00:00")
        );
    }

    private Place place(
            String buildingName,
            String placeName
    ) {
        Building building = new Building();
        ReflectionTestUtils.setField(building, "name", buildingName);

        Place place = new Place();
        ReflectionTestUtils.setField(place, "id", 10L);
        place.setBuilding(building);
        place.setName(placeName);
        return place;
    }
}
