package devkor.com.teamcback.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceTypeTest {

    @Test
    void supportsPharmacyPlaceType() {
        PlaceType pharmacy = PlaceType.valueOf("PHARMACY");

        assertThat(pharmacy.getName()).isEqualTo("약국");
        assertThat(pharmacy.getNickname()).isEmpty();
    }
}
