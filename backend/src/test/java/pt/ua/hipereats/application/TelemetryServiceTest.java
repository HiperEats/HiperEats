package pt.ua.hipereats.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase.Result;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

class TelemetryServiceTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-25T19:40:00Z");

    /** In-memory fake of the outbound port. */
    private static class FakeTelemetryRepository implements TelemetryRepository {
        final Map<String, TelemetryReading> latest = new HashMap<>();

        @Override
        public Optional<TelemetryReading> findLatest(String scooterCode) {
            return Optional.ofNullable(latest.get(scooterCode));
        }

        @Override
        public void saveLatest(TelemetryReading reading) {
            latest.put(reading.scooterCode(), reading);
        }
    }

    private final FakeTelemetryRepository repository = new FakeTelemetryRepository();
    private static final Duration MAX_FUTURE_SKEW = Duration.ofSeconds(30);
    private final Clock clock = Clock.fixed(T0.toInstant(), ZoneId.of("Europe/Lisbon"));
    private final TelemetryService service = new TelemetryService(repository, clock, MAX_FUTURE_SKEW);

    private static TelemetryReading reading(String code, int secondsAfterT0, int battery) {
        return new TelemetryReading(code, T0.plusSeconds(secondsAfterT0), battery, 40.64, -8.65, true, null);
    }

    @Test
    void whenFirstReadingOfAScooter_thenItIsRecorded() {
        Result result = service.record(reading("AV-01", 0, 80));

        assertThat(result).isEqualTo(Result.RECORDED);
        assertThat(service.latestFor("AV-01")).contains(reading("AV-01", 0, 80));
    }

    @Test
    void whenNewerReadingArrives_thenItReplacesThePrevious() {
        service.record(reading("AV-01", 0, 80));

        Result result = service.record(reading("AV-01", 5, 76));

        assertThat(result).isEqualTo(Result.RECORDED);
        assertThat(service.latestFor("AV-01")).get().extracting(TelemetryReading::batteryPercent).isEqualTo(76);
    }

    @Test
    void whenOlderReadingArrivesLate_thenItIsIgnored() {
        service.record(reading("AV-01", 5, 76));

        Result result = service.record(reading("AV-01", 0, 80));

        assertThat(result).isEqualTo(Result.IGNORED_STALE);
        assertThat(service.latestFor("AV-01")).get().extracting(TelemetryReading::batteryPercent).isEqualTo(76);
    }

    @Test
    void whenSameReadingArrivesTwice_thenTheDuplicateIsIgnored() {
        service.record(reading("AV-01", 0, 80));

        Result result = service.record(reading("AV-01", 0, 80));

        assertThat(result).isEqualTo(Result.IGNORED_STALE);
    }

    @Test
    void whenReadingsOfDifferentScooters_thenEachKeepsItsOwnState() {
        service.record(reading("AV-01", 5, 76));

        Result result = service.record(reading("AV-02", 0, 40));

        assertThat(result).isEqualTo(Result.RECORDED);
        assertThat(service.latestFor("AV-01")).get().extracting(TelemetryReading::batteryPercent).isEqualTo(76);
        assertThat(service.latestFor("AV-02")).get().extracting(TelemetryReading::batteryPercent).isEqualTo(40);
    }

    @Test
    void whenReadingIsAheadOfTheClockWithinTheTolerance_thenItIsRecorded() {
        Result result = service.record(reading("AV-01", 30, 76));

        assertThat(result).isEqualTo(Result.RECORDED);
    }

    @Test
    void whenReadingIsAheadOfTheClockBeyondTheTolerance_thenItIsIgnored() {
        Result result = service.record(reading("AV-01", 31, 76));

        assertThat(result).isEqualTo(Result.IGNORED_FUTURE);
        assertThat(service.latestFor("AV-01")).isEmpty();
    }

    @Test
    void whenAReadingFromTheFutureWasIgnored_thenLaterValidReadingsStillUpdateTheState() {
        service.record(new TelemetryReading("AV-01", T0.plusYears(10), 80, 40.64, -8.65, true, null));

        Result result = service.record(reading("AV-01", 0, 10));

        assertThat(result).isEqualTo(Result.RECORDED);
        assertThat(service.latestFor("AV-01")).get().extracting(TelemetryReading::batteryPercent).isEqualTo(10);
    }

    @Test
    void whenScooterHasNotReported_thenNoTelemetry() {
        assertThat(service.latestFor("AV-01")).isEmpty();
    }
}
