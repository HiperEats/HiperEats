package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;
import pt.ua.hipereats.domain.fleet.ScooterStateChangeRefusedException;

/** Inbound port: the operator retires a scooter beyond repair (HE-37). */
public interface RetireScooterUseCase {

    /**
     * Retires an inactive scooter; a retired scooter never changes state again.
     *
     * @throws ScooterNotFoundException           if no scooter has the code
     * @throws ScooterStateChangeRefusedException if the scooter is not inactive
     */
    Scooter retire(String code);
}
