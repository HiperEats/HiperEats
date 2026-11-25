package pt.ua.hipereats.simulator.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.ScooterType;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;
import pt.ua.hipereats.simulator.domain.Store;

class FleetFactoryTest {

    private static final List<Store> STORES = List.of(
            new Store("AV", "Aveiro", new GeoPoint(40.6405, -8.6538), 7, 3),
            new Store("IL", "Ílhavo", new GeoPoint(40.6005, -8.6669), 7, 3),
            new Store("AG", "Águeda", new GeoPoint(40.5747, -8.4483), 7, 3));

    private final FleetFactory factory = new FleetFactory(new Random(42), 4, 2, 20, 100);

    @Test
    void whenThreeStoresOfTen_thenThirtyScooters() {
        assertThat(factory.create(STORES)).hasSize(30);
    }

    @Test
    void whenStoreHasSevenStandardAndThreeLongRange_thenCodesFollowThatOrder() {
        List<SimulatedScooter> aveiro = factory.create(STORES).subList(0, 10);

        assertThat(aveiro).extracting(SimulatedScooter::code)
                .containsExactly("AV-01", "AV-02", "AV-03", "AV-04", "AV-05",
                        "AV-06", "AV-07", "AV-08", "AV-09", "AV-10");
        assertThat(aveiro.subList(0, 7)).allMatch(s -> s.type() == ScooterType.STANDARD);
        assertThat(aveiro.subList(7, 10)).allMatch(s -> s.type() == ScooterType.LONG_RANGE);
    }

    @Test
    void whenCreated_thenEveryScooterIsParkedAtItsOwnStore() {
        for (SimulatedScooter scooter : factory.create(STORES)) {
            assertThat(scooter.code()).startsWith(scooter.store().code() + "-");
            assertThat(scooter.position()).isEqualTo(scooter.store().location());
            assertThat(scooter.locked()).isTrue();
        }
    }

    @Test
    void whenCreated_thenInitialBatteryIsWithinTheConfiguredRange() {
        assertThat(factory.create(STORES))
                .extracting(SimulatedScooter::batteryPercent)
                .allMatch(battery -> battery >= 20 && battery <= 100);
    }
}
