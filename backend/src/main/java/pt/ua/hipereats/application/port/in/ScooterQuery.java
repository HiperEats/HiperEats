package pt.ua.hipereats.application.port.in;

import java.util.Optional;
import pt.ua.hipereats.domain.fleet.Scooter;

/** Inbound port: one scooter of the fleet, by its code. */
public interface ScooterQuery {

    Optional<Scooter> findByCode(String code);
}
