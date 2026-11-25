package pt.ua.hipereats.application.port.in;

import java.util.Optional;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Inbound port: the latest telemetry of a scooter, for the use cases that need its battery,
 * position or lock state (for example, hiding scooters without enough battery in HE-31).
 */
public interface ScooterTelemetryQuery {

    /** The latest reading of the scooter, or empty if it has not reported yet. */
    Optional<TelemetryReading> latestFor(String scooterCode);
}
