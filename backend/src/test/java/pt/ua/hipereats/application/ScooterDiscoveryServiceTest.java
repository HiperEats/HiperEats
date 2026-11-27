package pt.ua.hipereats.application;

import static org.assertj.core.api.Assertions.assertThat;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.application.port.in.AvailableScooter;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.fleet.RangeRule;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterType;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

class ScooterDiscoveryServiceTest {

    private static final long AVEIRO = 1L;
    private static final long ILHAVO = 2L;
    private static final BigDecimal ONE_KM = BigDecimal.ONE;

    /** Scooters kept in memory; the query on PostgreSQL is covered by the HE-31 scenarios. */
    private static final class InMemoryScooters implements ScooterRepository {
        private final List<Scooter> scooters = new ArrayList<>();

        @Override
        public void add(Scooter scooter) {
            scooters.add(scooter);
        }

        @Override
        public Optional<Scooter> findByCode(String code) {
            return scooters.stream().filter(s -> s.code().equals(code)).findFirst();
        }

        @Override
        public List<Scooter> findByStoreAndState(long storeId, ScooterState state) {
            return scooters.stream()
                    .filter(s -> s.storeId() == storeId && s.state() == state)
                    .toList();
        }
    }

    private static final class InMemoryTelemetry implements TelemetryRepository {
        private final Map<String, TelemetryReading> latest = new HashMap<>();

        @Override
        public Optional<TelemetryReading> findLatest(String scooterCode) {
            return Optional.ofNullable(latest.get(scooterCode));
        }

        @Override
        public void saveLatest(TelemetryReading reading) {
            latest.put(reading.scooterCode(), reading);
        }
    }

    private final InMemoryScooters scooters = new InMemoryScooters();
    private final InMemoryTelemetry telemetry = new InMemoryTelemetry();
    private final ScooterDiscoveryService service = new ScooterDiscoveryService(scooters, telemetry,
            new RangeRule(new BigDecimal("25"), new BigDecimal("50"), new BigDecimal("1.2")));

    private void scooter(String code, ScooterType type, ScooterState state, long storeId,
                         int batteryPercent) {
        scooters.add(new Scooter(code, type, state, storeId));
        telemetry.saveLatest(new TelemetryReading(code,
                OffsetDateTime.parse("2026-11-27T19:40:00.000Z"), batteryPercent,
                40.6405, -8.6538, true, null));
    }

    private List<String> codesAvailableAtAveiro(BigDecimal deliveryDistanceKm) {
        return service.availableFor(AVEIRO, deliveryDistanceKm).stream()
                .map(AvailableScooter::code)
                .toList();
    }

    @Test
    @Requirement("HE-31")
    void whenScooterBelongsToAnotherStore_thenItIsNotListed() {
        scooter("AV-01", ScooterType.STANDARD, ScooterState.AVAILABLE, AVEIRO, 80);
        scooter("IL-01", ScooterType.STANDARD, ScooterState.AVAILABLE, ILHAVO, 80);

        assertThat(codesAvailableAtAveiro(ONE_KM)).containsExactly("AV-01");
    }

    @Test
    @Requirement("HE-31")
    void whenScooterIsNotAvailable_thenItIsNotListed() {
        scooter("AV-01", ScooterType.STANDARD, ScooterState.AVAILABLE, AVEIRO, 80);
        scooter("AV-02", ScooterType.STANDARD, ScooterState.RESERVED, AVEIRO, 80);
        scooter("AV-03", ScooterType.STANDARD, ScooterState.MAINTENANCE, AVEIRO, 80);

        assertThat(codesAvailableAtAveiro(ONE_KM)).containsExactly("AV-01");
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: an available scooter without enough range for the delivery is not listed")
    void whenRangeDoesNotCoverTheDelivery_thenScooterIsNotListed() {
        scooter("AV-LR1", ScooterType.LONG_RANGE, ScooterState.AVAILABLE, AVEIRO, 15);
        scooter("AV-LR2", ScooterType.LONG_RANGE, ScooterState.AVAILABLE, AVEIRO, 14);

        assertThat(codesAvailableAtAveiro(new BigDecimal("3"))).containsExactly("AV-LR1");
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a scooter that has not reported its battery yet is not listed")
    void whenScooterHasNoTelemetry_thenItIsNotListed() {
        scooters.add(new Scooter("AV-01", ScooterType.STANDARD, ScooterState.AVAILABLE, AVEIRO));

        assertThat(codesAvailableAtAveiro(ONE_KM)).isEmpty();
    }

    @Test
    @Requirement("HE-31")
    void whenScooterIsListed_thenItShowsItsTypeAndBattery() {
        scooter("AV-LR", ScooterType.LONG_RANGE, ScooterState.AVAILABLE, AVEIRO, 60);

        assertThat(service.availableFor(AVEIRO, ONE_KM))
                .containsExactly(new AvailableScooter("AV-LR", ScooterType.LONG_RANGE, 60));
    }

    @Test
    @Requirement("HE-31")
    void whenSeveralScootersAreListed_thenTheyAreOrderedByCode() {
        scooter("AV-03", ScooterType.STANDARD, ScooterState.AVAILABLE, AVEIRO, 80);
        scooter("AV-01", ScooterType.STANDARD, ScooterState.AVAILABLE, AVEIRO, 80);
        scooter("AV-02", ScooterType.LONG_RANGE, ScooterState.AVAILABLE, AVEIRO, 80);

        assertThat(codesAvailableAtAveiro(ONE_KM)).containsExactly("AV-01", "AV-02", "AV-03");
    }
}
