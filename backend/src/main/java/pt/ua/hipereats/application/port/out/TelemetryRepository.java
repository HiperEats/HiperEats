package pt.ua.hipereats.application.port.out;

import java.util.Optional;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/** Outbound port: where the latest reading of each scooter is kept. */
public interface TelemetryRepository {

    Optional<TelemetryReading> findLatest(String scooterCode);

    /** Stores the reading as the scooter's latest one, replacing the previous. */
    void saveLatest(TelemetryReading reading);
}
