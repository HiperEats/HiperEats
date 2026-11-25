package pt.ua.hipereats.simulator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulatedScooterTest {

    private static final double SPEED_KMH = 20;
    private static final GeoPoint STORE_LOCATION = new GeoPoint(40.6405, -8.6538);
    private static final Store STORE = new Store("AV", "Aveiro", STORE_LOCATION, 7, 3);

    private static Trip tripOf(double oneWayKm, String fault) {
        return new Trip(STORE_LOCATION, STORE_LOCATION.destination(90, oneWayKm), fault);
    }

    private static SimulatedScooter standardScooter(double battery) {
        return new SimulatedScooter("AV-01", ScooterType.STANDARD, STORE, battery, 4);
    }

    @Test
    @DisplayName("BR12: a new scooter is parked and locked at its store")
    void whenCreated_thenParkedLockedAtStoreWithoutFault() {
        SimulatedScooter scooter = standardScooter(80);

        assertThat(scooter.locked()).isTrue();
        assertThat(scooter.position()).isEqualTo(STORE_LOCATION);
        assertThat(scooter.fault()).isNull();
        assertThat(scooter.onTrip()).isFalse();
    }

    @Test
    void whenParked_thenTimePassingChangesNothing() {
        SimulatedScooter scooter = standardScooter(80);

        scooter.advance(Duration.ofHours(1), SPEED_KMH);

        assertThat(scooter.batteryPercent()).isEqualTo(80);
        assertThat(scooter.position()).isEqualTo(STORE_LOCATION);
        assertThat(scooter.locked()).isTrue();
    }

    @Test
    @DisplayName("BR12: the unlock command unlocks the scooter and starts the trip")
    void whenUnlocked_thenUnlockedAndOnTrip() {
        SimulatedScooter scooter = standardScooter(80);

        boolean started = scooter.unlock(tripOf(3, null));

        assertThat(started).isTrue();
        assertThat(scooter.locked()).isFalse();
        assertThat(scooter.onTrip()).isTrue();
    }

    @Test
    void whenUnlockedTwice_thenSecondTripIsRefused() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, null));

        boolean startedAgain = scooter.unlock(tripOf(1, null));

        assertThat(startedAgain).isFalse();
    }

    @Test
    void whenRidingThreeMinutes_thenMovesAwayFromStore() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, null));

        scooter.advance(Duration.ofMinutes(3), SPEED_KMH);

        // 20 km/h for 3 min = 1 km
        assertThat(scooter.position().distanceKm(STORE_LOCATION)).isCloseTo(1.0, within(0.01));
        assertThat(scooter.locked()).isFalse();
    }

    @Test
    void whenStandardRides1Km_thenBatteryDrops4Points() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, null));

        scooter.advance(Duration.ofMinutes(3), SPEED_KMH);

        assertThat(scooter.batteryPercent()).isCloseTo(76, within(0.001));
    }

    @Test
    void whenLongRangeRides1Km_thenBatteryDrops2Points() {
        SimulatedScooter scooter = new SimulatedScooter("AV-08", ScooterType.LONG_RANGE, STORE, 80, 2);
        scooter.unlock(tripOf(3, null));

        scooter.advance(Duration.ofMinutes(3), SPEED_KMH);

        assertThat(scooter.batteryPercent()).isCloseTo(78, within(0.001));
    }

    @Test
    @DisplayName("BR6: after the round trip the scooter is locked back at its own store")
    void whenRoundTripEnds_thenLockedBackAtStore() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, null));

        // 3 km there and back at 20 km/h = 18 min
        scooter.advance(Duration.ofMinutes(18), SPEED_KMH);

        assertThat(scooter.locked()).isTrue();
        assertThat(scooter.onTrip()).isFalse();
        assertThat(scooter.position()).isEqualTo(STORE_LOCATION);
        assertThat(scooter.batteryPercent()).isCloseTo(80 - 6 * 4, within(0.01));
    }

    @Test
    void whenOneMinuteBeforeTheEnd_thenStillUnlocked() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, null));

        scooter.advance(Duration.ofMinutes(17), SPEED_KMH);

        assertThat(scooter.locked()).isFalse();
    }

    @Test
    void whenBatteryRunsOut_thenItStaysAtZero() {
        SimulatedScooter scooter = standardScooter(5);
        scooter.unlock(tripOf(3, null));

        scooter.advance(Duration.ofMinutes(18), SPEED_KMH);

        assertThat(scooter.batteryPercent()).isZero();
    }

    @Test
    @DisplayName("BR8: a faulty trip reports the fault from the delivery address on")
    void whenFaultyTripReachesDestination_thenFaultIsReported() {
        SimulatedScooter scooter = standardScooter(80);
        scooter.unlock(tripOf(3, "MOTOR_FAULT"));

        scooter.advance(Duration.ofMinutes(8), SPEED_KMH);
        assertThat(scooter.fault()).isNull();

        scooter.advance(Duration.ofMinutes(2), SPEED_KMH);
        assertThat(scooter.fault()).isEqualTo("MOTOR_FAULT");

        scooter.advance(Duration.ofMinutes(8), SPEED_KMH);
        assertThat(scooter.fault()).isEqualTo("MOTOR_FAULT");
        assertThat(scooter.locked()).isTrue();
    }
}
