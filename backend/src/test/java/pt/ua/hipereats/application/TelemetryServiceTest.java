package pt.ua.hipereats.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
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
    private final TelemetryService service = new TelemetryService(repository);

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
    void whenScooterHasNotReported_thenNoTelemetry() {
        assertThat(service.latestFor("AV-01")).isEmpty();
    }
}
