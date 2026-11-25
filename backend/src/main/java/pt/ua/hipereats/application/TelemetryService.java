package pt.ua.hipereats.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * <p>A reading dated too far in the future is also ignored. Otherwise one wrong clock would make
 * every later reading of that scooter look stale, and its state would stop updating.
 */
@Service
public class TelemetryService implements RecordTelemetryUseCase, ScooterTelemetryQuery {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryRepository repository;
    private final Clock clock;
    private final Duration maxFutureSkew;

    /**
     * @param maxFutureSkew how far ahead of the platform clock a reading may be dated (the two clocks
     *                      are never exactly in sync)
     */
    public TelemetryService(TelemetryRepository repository, Clock clock,
                            @Value("${hipereats.telemetry.max-future-skew:30s}") Duration maxFutureSkew) {
        this.repository = repository;
        this.clock = clock;
        this.maxFutureSkew = maxFutureSkew;
    }

    @Override
    public synchronized Result record(TelemetryReading reading) {
        Instant latestAccepted = clock.instant().plus(maxFutureSkew);
        if (reading.timestamp().toInstant().isAfter(latestAccepted)) {
            log.warn("Reading of {} dated in the future ({}) ignored", reading.scooterCode(), reading.timestamp());
            return Result.IGNORED_FUTURE;
        }
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
