package pt.ua.hipereats.simulator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoPointTest {

    private static final GeoPoint AVEIRO = new GeoPoint(40.6405, -8.6538);

    @Test
    void whenSamePoint_thenDistanceIsZero() {
        assertThat(AVEIRO.distanceKm(AVEIRO)).isZero();
    }

    @Test
    void whenOneDegreeOfLatitudeApart_thenDistanceIsAbout111Km() {
        GeoPoint north = new GeoPoint(41.6405, -8.6538);

        assertThat(AVEIRO.distanceKm(north)).isCloseTo(111.2, within(0.1));
    }

    @Test
    void whenTravellingAnyDirection_thenDestinationIsAtThatDistance() {
        for (double bearing = 0; bearing < 360; bearing += 45) {
            GeoPoint destination = AVEIRO.destination(bearing, 2.5);

            assertThat(AVEIRO.distanceKm(destination)).isCloseTo(2.5, within(0.001));
        }
    }

    @Test
    void whenTravellingNorth_thenLatitudeGrowsAndLongitudeStays() {
        GeoPoint destination = AVEIRO.destination(0, 1);

        assertThat(destination.latitude()).isGreaterThan(AVEIRO.latitude());
        assertThat(destination.longitude()).isCloseTo(AVEIRO.longitude(), within(1e-9));
    }

    @Test
    void whenGoingHalfway_thenPointIsInTheMiddle() {
        GeoPoint to = new GeoPoint(40.6505, -8.6438);

        GeoPoint middle = AVEIRO.towards(to, 0.5);

        assertThat(middle.latitude()).isCloseTo(40.6455, within(1e-9));
        assertThat(middle.longitude()).isCloseTo(-8.6488, within(1e-9));
    }
}
