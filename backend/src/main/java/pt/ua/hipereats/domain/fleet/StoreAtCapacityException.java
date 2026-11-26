package pt.ua.hipereats.domain.fleet;

/** BR13: the store already holds as many scooters as its fleet capacity. */
public class StoreAtCapacityException extends RuntimeException {

    public StoreAtCapacityException(long storeId, int fleetCapacity) {
        super("BR13: store " + storeId + " is at its fleet capacity of " + fleetCapacity
                + " scooters");
    }
}
