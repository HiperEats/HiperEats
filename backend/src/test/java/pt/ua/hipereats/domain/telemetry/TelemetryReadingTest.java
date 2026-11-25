package pt.ua.hipereats.domain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelemetryReadingTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-25T19:40:00Z");

    private static TelemetryReading reading(int battery, double latitude, double longitude) {
        return new TelemetryReading("AV-01", T0, battery, latitude, longitude, true, null);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 15, 100})
    void whenBatteryWithin0And100_thenReadingIsAccepted(int battery) {
        assertThatNoException().isThrownBy(() -> reading(battery, 40.64, -8.65));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void whenBatteryOutside0And100_thenReadingIsRefused(int battery) {
        assertThatIllegalArgumentException().isThrownBy(() -> reading(battery, 40.64, -8.65));
    }

    @Test
    void whenCoordinatesOutOfRange_thenReadingIsRefused() {
        assertThatIllegalArgumentException().isThrownBy(() -> reading(50, 90.01, -8.65));
        assertThatIllegalArgumentException().isThrownBy(() -> reading(50, 40.64, -180.01));
    }

    @Test
    void whenCoordinatesAtTheLimits_thenReadingIsAccepted() {
        assertThatNoException().isThrownBy(() -> reading(50, 90, 180));
        assertThatNoException().isThrownBy(() -> reading(50, -90, -180));
    }

    @Test
    void whenScooterCodeOrTimestampMissing_thenReadingIsRefused() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new TelemetryReading(" ", T0, 50, 40.64, -8.65, true, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new TelemetryReading("AV-01", null, 50, 40.64, -8.65, true, null));
    }

    @Test
    void whenTakenLater_thenReadingIsNewer() {
        TelemetryReading earlier = reading(50, 40.64, -8.65);
        TelemetryReading later = new TelemetryReading("AV-01", T0.plusSeconds(1), 50, 40.64, -8.65, true, null);

        assertThat(later.isNewerThan(earlier)).isTrue();
        assertThat(earlier.isNewerThan(later)).isFalse();
        assertThat(earlier.isNewerThan(earlier)).isFalse();
    }

    @Test
    void whenSameInstantInAnotherOffset_thenReadingIsNotNewer() {
        TelemetryReading utc = reading(50, 40.64, -8.65);
        TelemetryReading sameInstantPlusOne = new TelemetryReading("AV-01",
                OffsetDateTime.parse("2026-11-25T20:40:00+01:00"), 50, 40.64, -8.65, true, null);

        assertThat(sameInstantPlusOne.isNewerThan(utc)).isFalse();
    }

    @Test
    void whenFaultReported_thenReadingHasFault() {
        TelemetryReading faulty = new TelemetryReading("AV-01", T0, 50, 40.64, -8.65, true, "MOTOR_FAULT");

        assertThat(faulty.hasFault()).isTrue();
        assertThat(reading(50, 40.64, -8.65).hasFault()).isFalse();
    }
}
