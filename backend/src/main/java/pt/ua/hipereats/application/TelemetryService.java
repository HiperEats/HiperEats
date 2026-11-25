package pt.ua.hipereats.application;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase;
import pt.ua.hipereats.application.port.in.ScooterTelemetryQuery;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Keeps the latest telemetry of each scooter.
 *
 * <p>Telemetry can arrive late or twice (Product Specification, 4.1), so a reading that is not
 * newer than the one already kept is ignored: an old reading never overwrites a newer state.
 */
@Service
public class TelemetryService implements RecordTelemetryUseCase, ScooterTelemetryQuery {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryRepository repository;

    public TelemetryService(TelemetryRepository repository) {
        this.repository = repository;
    }

    @Override
    public synchronized Result record(TelemetryReading reading) {
        Optional<TelemetryReading> latest = repository.findLatest(reading.scooterCode());
        if (latest.isPresent() && !reading.isNewerThan(latest.get())) {
            log.debug("Stale reading of {} at {} ignored", reading.scooterCode(), reading.timestamp());
            return Result.IGNORED_STALE;
        }
        repository.saveLatest(reading);
        return Result.RECORDED;
    }

    @Override
    public Optional<TelemetryReading> latestFor(String scooterCode) {
        return repository.findLatest(scooterCode);
    }
}
