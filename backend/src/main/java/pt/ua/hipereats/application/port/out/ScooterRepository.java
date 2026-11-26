package pt.ua.hipereats.application.port.out;

import java.util.Optional;
import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;

/** Outbound port: the scooters of the fleet. */
public interface ScooterRepository {

    /**
     * Stores a new scooter.
     *
     * @throws ScooterCodeAlreadyUsedException if another scooter already has its code
     */
    void add(Scooter scooter);

    Optional<Scooter> findByCode(String code);

    /**
     * Loads the scooter and locks it until the transaction ends, so that changes of its state run
     * one after the other.
     */
    Optional<Scooter> findAndLock(String code);

    /**
     * Stores the new state of an existing scooter, only over the state the change started from.
     *
     * @param from the state the scooter had when the change read it
     * @throws org.springframework.dao.OptimisticLockingFailureException if the stored state is no
     *                                                                   longer {@code from}, or the
     *                                                                   scooter does not exist
     */
    void update(Scooter scooter, ScooterState from);
}
