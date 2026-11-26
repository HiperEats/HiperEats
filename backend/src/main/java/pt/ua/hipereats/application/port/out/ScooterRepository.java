package pt.ua.hipereats.application.port.out;

import java.util.Optional;
import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.Scooter;

/** Outbound port: the scooters of the fleet. */
public interface ScooterRepository {

    /**
     * Stores a new scooter.
     *
     * @throws ScooterCodeAlreadyUsedException if another scooter already has its code
     */
    void add(Scooter scooter);

    Optional<Scooter> findByCode(String code);
}
