package pt.ua.hipereats.domain.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreTest {

    @Test
    @Requirement("HE-29")
    @DisplayName("BR13: the scooter that fills the store is accepted")
    void whenStoreHasOneFreePlace_thenScooterIsAccepted() {
        Store store = new Store(7L, 10, 9);

        Scooter scooter = store.registerScooter("AV-10", ScooterType.STANDARD);

        assertThat(scooter.storeId()).isEqualTo(7L);
    }

    @Test
    @Requirement("HE-29")
    @DisplayName("BR13: a scooter beyond the store capacity is refused")
    void whenStoreIsAtCapacity_thenScooterIsRefused() {
        Store store = new Store(7L, 10, 10);

        assertThatThrownBy(() -> store.registerScooter("AV-11", ScooterType.STANDARD))
                .isInstanceOf(StoreAtCapacityException.class)
                .hasMessageStartingWith("BR13");
    }

    @Test
    @Requirement("HE-29")
    @DisplayName("BR13: a store above its capacity refuses another scooter")
    void whenStoreIsAboveCapacity_thenScooterIsRefused() {
        Store store = new Store(7L, 10, 11);

        assertThatThrownBy(() -> store.registerScooter("AV-12", ScooterType.STANDARD))
                .isInstanceOf(StoreAtCapacityException.class);
    }

    @Test
    @Requirement("HE-29")
    @DisplayName("BR13: a registered scooter starts inactive")
    void whenScooterIsRegistered_thenItStartsInactive() {
        Store store = new Store(7L, 10, 0);

        Scooter scooter = store.registerScooter("AV-01", ScooterType.STANDARD);

        assertThat(scooter.state()).isEqualTo(ScooterState.INACTIVE);
    }

    @Test
    @Requirement("HE-29")
    void whenScooterIsRegistered_thenItKeepsItsCodeAndType() {
        Store store = new Store(7L, 10, 0);

        Scooter scooter = store.registerScooter("AV-08", ScooterType.LONG_RANGE);

        assertThat(scooter.code()).isEqualTo("AV-08");
        assertThat(scooter.type()).isEqualTo(ScooterType.LONG_RANGE);
    }
}
