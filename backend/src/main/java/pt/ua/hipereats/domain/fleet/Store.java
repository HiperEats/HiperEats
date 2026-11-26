package pt.ua.hipereats.domain.fleet;

/**
 * A store of the chain, seen from its fleet.
 *
 * @param fleetCapacity the most scooters the store may hold, set by the client (BR13)
 * @param scooterCount  how many scooters are registered at the store now
 */
public record Store(long id, int fleetCapacity, int scooterCount) {

    /**
     * Registers a new scooter in this store's fleet. The scooter starts inactive.
     *
     * @throws StoreAtCapacityException if the store already holds its fleet capacity (BR13)
     */
    public Scooter registerScooter(String code, ScooterType type) {
        if (scooterCount >= fleetCapacity) {
            throw new StoreAtCapacityException(id, fleetCapacity);
        }
        return new Scooter(code, type, ScooterState.INACTIVE, id);
    }
}
