package pt.ua.hipereats.domain.fleet;

/**
 * A store of the chain, seen from its fleet.
 *
 * @param fleetCapacity the most scooters the store may hold, set by the client (BR13)
 * @param scooterCount  how many scooters are registered at the store now
 */
public record Store(long id, int fleetCapacity, int scooterCount) {

    /** Registers a new scooter in this store's fleet. */
    public Scooter registerScooter(String code, ScooterType type) {
        throw new UnsupportedOperationException("HE-29: not implemented yet");
    }
}
