package pt.ua.hipereats.simulator.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.ScooterType;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;
import pt.ua.hipereats.simulator.domain.Store;
import pt.ua.hipereats.simulator.domain.Trip;

class TripPlannerTest {

    private static final int SAMPLES = 20_000;
    private static final Store STORE = new Store("AV", "Aveiro", new GeoPoint(40.6405, -8.6538), 1, 1);
    /** Rounding margin of the distance computed back from the destination coordinates. */
    private static final double KM_TOLERANCE = 1e-6;

    private final TripPlanner planner = planner(new Random(42), 0.02);

    private static TripPlanner planner(Random random, double faultProbability) {
        return new TripPlanner(random, 0.60, 0.35, faultProbability, 2.0, 3.0, 1.2);
    }

    private static SimulatedScooter standard(double battery) {
        return new SimulatedScooter("AV-01", ScooterType.STANDARD, STORE, battery, 4);
    }

    private static SimulatedScooter longRange(double battery) {
        return new SimulatedScooter("AV-08", ScooterType.LONG_RANGE, STORE, battery, 2);
    }

    private static double oneWayKm(Trip trip) {
        return trip.totalKm() / 2;
    }

    @Test
    void whenManyOrdersAreDrawn_thenTheyFollowTheDistanceShares() {
        int near = 0;
        int middle = 0;
        int far = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double km = planner.deliveryDistanceKm();
            if (km < 2) {
                near++;
            } else if (km < 3) {
                middle++;
            } else {
                far++;
            }
        }

        assertThat((double) near / SAMPLES).isCloseTo(0.60, within(0.02));
        assertThat((double) middle / SAMPLES).isCloseTo(0.35, within(0.02));
        assertThat((double) far / SAMPLES).isCloseTo(0.05, within(0.01));
    }

    @Test
    @DisplayName("BR14: a standard scooter never rides beyond 2 km")
    void whenStandardScooterWithFullBattery_thenEveryTripIsWithin2Km() {
        SimulatedScooter scooter = standard(100);

        for (int i = 0; i < SAMPLES; i++) {
            assertThat(oneWayKm(planner.plan(scooter))).isBetween(TripPlanner.MIN_KM, 2.0 + KM_TOLERANCE);
        }
    }

    @Test
    @DisplayName("BR2: a long-range scooter rides up to the 3 km radius, never beyond")
    void whenLongRangeScooterWithFullBattery_thenTripsGoUpTo3Km() {
        SimulatedScooter scooter = longRange(100);
        int beyond2Km = 0;

        for (int i = 0; i < SAMPLES; i++) {
            double km = oneWayKm(planner.plan(scooter));
            assertThat(km).isBetween(TripPlanner.MIN_KM, 3.0 + KM_TOLERANCE);
            if (km > 2.0) {
                beyond2Km++;
            }
        }

        assertThat(beyond2Km).isPositive();
    }

    @Test
    @DisplayName("BR1: the trip never needs more than the range, with the 20 % margin")
    void whenBatteryIsLow_thenTripStaysWithinTheRangeWithMargin() {
        SimulatedScooter scooter = standard(10); // 10 % of 25 km = 2.5 km of range

        for (int i = 0; i < SAMPLES; i++) {
            double km = oneWayKm(planner.plan(scooter));
            assertThat(2 * km * 1.2).isLessThanOrEqualTo(2.5 + KM_TOLERANCE);
        }
    }

    @Test
    @DisplayName("BR1: a trip planned within the range never empties the battery")
    void whenLowBatteryScooterRidesItsTrip_thenBatteryNeverReachesZero() {
        for (int i = 0; i < 1_000; i++) {
            SimulatedScooter scooter = standard(10);
            scooter.unlock(planner.plan(scooter));
            scooter.advance(java.time.Duration.ofHours(1), 20);

            assertThat(scooter.batteryPercent()).isPositive();
        }
    }

    @Test
    void whenManyTripsArePlanned_thenAbout2PercentHaveAFault() {
        int faulty = 0;
        for (int i = 0; i < SAMPLES; i++) {
            Trip trip = planner.plan(longRange(100));
            if (trip.fault() != null) {
                assertThat(TripPlanner.FAULT_CODES).contains(trip.fault());
                faulty++;
            }
        }

        assertThat((double) faulty / SAMPLES).isCloseTo(0.02, within(0.005));
    }

    @Test
    void whenTheSameSeedIsUsed_thenTheSameTripIsPlanned() {
        Trip first = planner(new Random(7), 0.02).plan(standard(100));
        Trip second = planner(new Random(7), 0.02).plan(standard(100));

        assertThat(first.destination()).isEqualTo(second.destination());
    }
}
