package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.StoreNotFoundException;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.StoreAtCapacityException;

/** Inbound port: the operator registers a scooter at a store (HE-29). */
public interface RegisterScooterUseCase {

    /**
     * Registers the scooter in the store's fleet; it starts inactive.
     *
     * @throws StoreNotFoundException          if the store does not exist
     * @throws StoreAtCapacityException        if the store is at its fleet capacity (BR13)
     * @throws ScooterCodeAlreadyUsedException if another scooter already has the code
     */
    Scooter register(RegisterScooterCommand command);
}
