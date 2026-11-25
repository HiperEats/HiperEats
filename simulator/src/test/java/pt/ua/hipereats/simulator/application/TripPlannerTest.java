package pt.ua.hipereats.simulator.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.Trip;

class TripPlannerTest {

    private static final int SAMPLES = 20_000;
    private static final GeoPoint STORE = new GeoPoint(40.6405, -8.6538);

    private final TripPlanner planner = new TripPlanner(new Random(42), 0.60, 0.35, 0.02);

    @Test
    void whenManyDeliveriesArePlanned_thenTheyFollowTheDistanceShares() {
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
    void whenDeliveriesArePlanned_thenDistancesStayWithinBounds() {
        for (int i = 0; i < SAMPLES; i++) {
            assertThat(planner.deliveryDistanceKm()).isBetween(TripPlanner.MIN_KM, TripPlanner.FAR_MAX_KM);
        }
    }

    @Test
    void whenManyTripsArePlanned_thenAbout2PercentHaveAFault() {
        int faulty = 0;
        for (int i = 0; i < SAMPLES; i++) {
            Trip trip = planner.plan(STORE);
            if (trip.fault() != null) {
                assertThat(TripPlanner.FAULT_CODES).contains(trip.fault());
                faulty++;
            }
        }

        assertThat((double) faulty / SAMPLES).isCloseTo(0.02, within(0.005));
    }

    @Test
    void whenTheSameSeedIsUsed_thenTheSameTripIsPlanned() {
        Trip first = new TripPlanner(new Random(7), 0.60, 0.35, 0.02).plan(STORE);
        Trip second = new TripPlanner(new Random(7), 0.60, 0.35, 0.02).plan(STORE);

        assertThat(first.destination()).isEqualTo(second.destination());
    }
}
