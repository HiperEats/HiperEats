package pt.ua.hipereats.simulator.domain;

import java.time.Duration;

/**
 * The simulated hardware of one scooter: battery, GPS position, lock and fault sensor.
 *
 * <p>It is parked and locked at its store until the platform unlocks it (BR12). Then it rides a
 * {@link Trip}, spending battery per km, and comes back to its store, where it locks again.
 */
public final class SimulatedScooter {

    private final String code;
    private final ScooterType type;
    private final Store store;
    private final double batteryUsePerKm;
    private double batteryPercent;
    private GeoPoint position;
    private boolean locked = true;
    private String fault;
    private Trip trip;

    public SimulatedScooter(String code, ScooterType type, Store store, double batteryPercent, double batteryUsePerKm) {
        this.code = code;
        this.type = type;
        this.store = store;
        this.batteryPercent = batteryPercent;
        this.batteryUsePerKm = batteryUsePerKm;
        this.position = store.location();
    }

    /**
     * Unlocks the scooter and starts the trip. Returns false, and changes nothing, if it is already
     * on a trip: a repeated unlock command is harmless.
     */
    public boolean unlock(Trip newTrip) {
        if (trip != null) {
            return false;
        }
        trip = newTrip;
        locked = false;
        return true;
    }

    /** Moves the scooter along its trip for {@code elapsed} time at {@code speedKmh}. A parked scooter does not change. */
    public void advance(Duration elapsed, double speedKmh) {
        if (trip == null) {
            return;
        }
        double km = speedKmh * elapsed.toMillis() / 3_600_000.0;
        double travelled = trip.advance(km);
        batteryPercent = Math.max(0, batteryPercent - travelled * batteryUsePerKm);
        position = trip.position();
        if (trip.fault() != null && trip.reachedDestination() && fault == null) {
            fault = trip.fault();
        }
        if (trip.finished()) {
            position = store.location();
            locked = true;
            trip = null;
        }
    }

    /** Battery level as the scooter reports it: an integer, rounded down. */
    public int reportedBatteryPercent() {
        return (int) Math.floor(batteryPercent);
    }

    /** Kilometres the battery still allows: at 100 % the full range of the type (BR1: 25 km or 50 km). */
    public double estimatedRangeKm() {
        return batteryPercent / batteryUsePerKm;
    }

    public boolean onTrip() {
        return trip != null;
    }

    public String code() {
        return code;
    }

    public ScooterType type() {
        return type;
    }

    public Store store() {
        return store;
    }

    public double batteryPercent() {
        return batteryPercent;
    }

    public GeoPoint position() {
        return position;
    }

    public boolean locked() {
        return locked;
    }

    public String fault() {
        return fault;
    }
}
