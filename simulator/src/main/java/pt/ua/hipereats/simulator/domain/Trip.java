package pt.ua.hipereats.simulator.domain;

/**
 * A delivery trip: from the store to the delivery address and back, in a straight line.
 * The scooter always comes back to its own store (BR6).
 */
public final class Trip {

    private final GeoPoint store;
    private final GeoPoint destination;
    private final double oneWayKm;
    private final String fault;
    private double travelledKm;

    /**
     * @param store       where the trip starts and ends
     * @param destination the delivery address
     * @param fault       fault code the scooter reports when it reaches the destination, or null for none
     */
    public Trip(GeoPoint store, GeoPoint destination, String fault) {
        this.store = store;
        this.destination = destination;
        this.oneWayKm = store.distanceKm(destination);
        this.fault = fault;
    }

    /** Moves up to {@code km} along the trip and returns the km actually travelled (less at the end of the trip). */
    public double advance(double km) {
        double travelled = Math.min(km, totalKm() - travelledKm);
        travelledKm += travelled;
        return travelled;
    }

    public GeoPoint position() {
        if (oneWayKm == 0) {
            return store;
        }
        if (travelledKm <= oneWayKm) {
            return store.towards(destination, travelledKm / oneWayKm);
        }
        return destination.towards(store, (travelledKm - oneWayKm) / oneWayKm);
    }

    public boolean reachedDestination() {
        return travelledKm >= oneWayKm;
    }

    public boolean finished() {
        return travelledKm >= totalKm();
    }

    public double totalKm() {
        return 2 * oneWayKm;
    }

    public String fault() {
        return fault;
    }

    public GeoPoint destination() {
        return destination;
    }
}
