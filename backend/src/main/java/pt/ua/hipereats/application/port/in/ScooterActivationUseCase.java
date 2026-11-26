package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;
import pt.ua.hipereats.domain.fleet.ScooterStateChangeRefusedException;

/** Inbound port: the operator activates and deactivates a scooter (HE-30). */
public interface ScooterActivationUseCase {

    /**
     * Makes an inactive scooter available.
     *
     * @throws ScooterNotFoundException           if no scooter has the code
     * @throws ScooterStateChangeRefusedException if the scooter is not inactive
     */
    Scooter activate(String code);

    /**
     * Makes an available scooter, or one in maintenance, inactive.
     *
     * @throws ScooterNotFoundException           if no scooter has the code
     * @throws ScooterStateChangeRefusedException if the scooter is in any other state (in use, for
     *                                            example)
     */
    Scooter deactivate(String code);
}
