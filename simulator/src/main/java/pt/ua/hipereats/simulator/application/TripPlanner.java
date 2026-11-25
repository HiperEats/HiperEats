package pt.ua.hipereats.simulator.application;

import java.util.List;
import java.util.random.RandomGenerator;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.Trip;

/**
 * Chooses the delivery of each trip with the distribution of the Product Specification (4.3):
 * by default 60 % within 2 km, 35 % between 2 and 3 km and 5 % beyond 3 km, in a random direction;
 * 2 % of the uses report a fault.
 */
public class TripPlanner {

    /** Shortest delivery: a trip to the store's own door makes no sense. */
    static final double MIN_KM = 0.2;
    /** Far deliveries (beyond the 3 km radius of BR2) fall between 3 and this value. */
    static final double FAR_MAX_KM = 3.5;
    static final List<String> FAULT_CODES = List.of("BRAKE_FAULT", "MOTOR_FAULT", "BATTERY_FAULT");

    private final RandomGenerator random;
    private final double nearShare;
    private final double middleShare;
    private final double faultProbability;

    public TripPlanner(RandomGenerator random, double nearShare, double middleShare, double faultProbability) {
        this.random = random;
        this.nearShare = nearShare;
        this.middleShare = middleShare;
        this.faultProbability = faultProbability;
    }

    public Trip plan(GeoPoint store) {
        double km = deliveryDistanceKm();
        double bearing = random.nextDouble(360);
        String fault = random.nextDouble() < faultProbability
                ? FAULT_CODES.get(random.nextInt(FAULT_CODES.size()))
                : null;
        return new Trip(store, store.destination(bearing, km), fault);
    }

    double deliveryDistanceKm() {
        double band = random.nextDouble();
        if (band < nearShare) {
            return random.nextDouble(MIN_KM, 2.0);
        }
        if (band < nearShare + middleShare) {
            return random.nextDouble(2.0, 3.0);
        }
        return random.nextDouble(3.0, FAR_MAX_KM);
    }
}
