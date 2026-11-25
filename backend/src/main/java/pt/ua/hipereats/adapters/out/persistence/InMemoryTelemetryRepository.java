package pt.ua.hipereats.adapters.out.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Keeps the latest reading of each scooter in memory.
 *
 * <p>Temporary: when the database (HE-45) and the scooter entity (HE-29) are in place, a JPA
 * adapter replaces this one behind the same port, with no change to the use case.
 */
@Repository
public class InMemoryTelemetryRepository implements TelemetryRepository {

    private final Map<String, TelemetryReading> latest = new ConcurrentHashMap<>();

    @Override
    public Optional<TelemetryReading> findLatest(String scooterCode) {
        return Optional.ofNullable(latest.get(scooterCode));
    }

    @Override
    public void saveLatest(TelemetryReading reading) {
        latest.put(reading.scooterCode(), reading);
    }
}
