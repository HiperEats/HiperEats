package pt.ua.hipereats.simulator.application;

import java.util.List;
import java.util.random.RandomGenerator;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;
import pt.ua.hipereats.simulator.domain.Trip;

/**
 * Chooses the delivery of each trip.
 *
 * <p>Orders follow the distribution of the Product Specification (4.3): by default 60 % within 2 km,
 * 35 % between 2 and 3 km and 5 % beyond 3 km. But a scooter only rides deliveries the platform would
 * accept for it, so each trip is an order drawn from that distribution that also respects:
 * <ul>
 *   <li>BR14: up to 2 km for a standard scooter;</li>
 *   <li>BR2: up to the 3 km delivery radius for a long-range scooter;</li>
 *   <li>BR1: the round trip plus the 20 % margin fits in the scooter's estimated range.</li>
 * </ul>
 * 2 % of the trips report a fault.
 */
public class TripPlanner {

    /** Shortest delivery: a trip to the store's own door makes no sense. */
    static final double MIN_KM = 0.2;
    /** Orders beyond the delivery radius (refused by BR2) reach up to this much farther. */
    static final double FAR_EXTRA_KM = 0.5;
    /** Draws before giving up on the distribution (only when the allowed distance is tiny). */
    private static final int MAX_DRAWS = 1_000;
    static final List<String> FAULT_CODES = List.of("BRAKE_FAULT", "MOTOR_FAULT", "BATTERY_FAULT");

    private final RandomGenerator random;
    private final double nearShare;
    private final double middleShare;
    private final double faultProbability;
    private final double standardMaxKm;
    private final double longRangeMaxKm;
    private final double rangeMargin;

    /**
     * @param standardMaxKm  farthest delivery of a standard scooter (BR14, 2 km)
     * @param longRangeMaxKm farthest delivery of a long-range scooter, the delivery radius (BR2, 3 km)
     * @param rangeMargin    required range = 2 x distance x this margin (BR1, 1.2)
     */
    public TripPlanner(RandomGenerator random, double nearShare, double middleShare, double faultProbability,
                       double standardMaxKm, double longRangeMaxKm, double rangeMargin) {
        this.random = random;
        this.nearShare = nearShare;
        this.middleShare = middleShare;
        this.faultProbability = faultProbability;
        this.standardMaxKm = standardMaxKm;
        this.longRangeMaxKm = longRangeMaxKm;
        this.rangeMargin = rangeMargin;
    }

    public Trip plan(SimulatedScooter scooter) {
        double km = tripDistanceKm(maxKmFor(scooter));
        double bearing = random.nextDouble(360);
        String fault = random.nextDouble() < faultProbability
                ? FAULT_CODES.get(random.nextInt(FAULT_CODES.size()))
                : null;
        var store = scooter.store().location();
        return new Trip(store, store.destination(bearing, km), fault);
    }

    /** Farthest delivery the platform would accept for this scooter (BR1, BR2, BR14). */
    double maxKmFor(SimulatedScooter scooter) {
        double byType = switch (scooter.type()) {
            case STANDARD -> standardMaxKm;
            case LONG_RANGE -> longRangeMaxKm;
        };
        double byBattery = scooter.estimatedRangeKm() / (2 * rangeMargin);
        return Math.min(byType, byBattery);
    }

    /** An order distance up to {@code maxKm}, keeping the shape of the order distribution. */
    private double tripDistanceKm(double maxKm) {
        if (maxKm <= MIN_KM) {
            // The platform should not have unlocked it; ride the little that is possible.
            return Math.max(0, maxKm);
        }
        for (int i = 0; i < MAX_DRAWS; i++) {
            double km = deliveryDistanceKm();
            if (km <= maxKm) {
                return km;
            }
        }
        return between(MIN_KM, maxKm);
    }

    /** The distance of one order, as the order generator would produce it (may be refused by BR2). */
    double deliveryDistanceKm() {
        double band = random.nextDouble();
        if (band < nearShare) {
            return between(MIN_KM, standardMaxKm);
        }
        if (band < nearShare + middleShare) {
            return between(standardMaxKm, longRangeMaxKm);
        }
        return between(longRangeMaxKm, longRangeMaxKm + FAR_EXTRA_KM);
    }

    /** A random value of the band, or {@code min} when the configured limits leave the band empty. */
    private double between(double min, double max) {
        return min < max ? random.nextDouble(min, max) : min;
    }
}
