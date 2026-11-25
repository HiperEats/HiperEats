package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;
import tools.jackson.core.JacksonException;

/** Contract tests: the JSON of AGENTS.md section 6, as the simulator sends it, becomes a domain reading. */
class TelemetryMessageTest {

    private static TelemetryMessage parse(String payload) {
        return TelemetryMessage.parse(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void whenContractMessage_thenItBecomesAReading() {
        TelemetryMessage message = parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":false,"fault":"MOTOR_FAULT"}
                """);

        TelemetryReading reading = message.toReading();

        assertThat(reading).isEqualTo(new TelemetryReading("AV-01",
                OffsetDateTime.parse("2026-11-25T19:40:05Z"), 76, 40.6405, -8.6538, false, "MOTOR_FAULT"));
    }

    @Test
    void whenFaultIsNull_thenReadingHasNoFault() {
        TelemetryMessage message = parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """);

        assertThat(message.toReading().hasFault()).isFalse();
    }

    @Test
    void whenExtraFields_thenTheyAreIgnored() {
        TelemetryMessage message = parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null,"speedKmh":20}
                """);

        assertThat(message.toReading().scooterCode()).isEqualTo("AV-01");
    }

    @Test
    void whenAFieldIsMissing_thenMessageIsRefused() {
        TelemetryMessage message = parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z",
                 "latitude":40.6405,"longitude":-8.6538,"locked":true}
                """);

        assertThatIllegalArgumentException().isThrownBy(message::toReading);
    }

    @Test
    void whenTimestampHasNoOffset_thenMessageIsRefused() {
        TelemetryMessage message = parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """);

        assertThatThrownBy(message::toReading).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void whenNumberComesAsText_thenMessageIsRefused() {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":"76",
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """)).isInstanceOf(JacksonException.class);
    }

    @Test
    void whenLatitudeIsTheTextNaN_thenMessageIsRefused() {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":"NaN","longitude":-8.6538,"locked":true,"fault":null}
                """).toReading()).isInstanceOfAny(JacksonException.class, IllegalArgumentException.class);
    }

    @Test
    void whenBatteryHasDecimals_thenMessageIsRefused() {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":100.9,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """)).isInstanceOf(JacksonException.class);
    }

    @Test
    void whenLockedIsANumber_thenMessageIsRefused() {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":1,"fault":null}
                """)).isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1.5", "true"})
    void whenFaultIsNotText_thenMessageIsRefused(String fault) {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":%s}
                """.formatted(fault))).isInstanceOf(JacksonException.class);
    }

    @Test
    void whenScooterCodeIsANumber_thenMessageIsRefused() {
        assertThatThrownBy(() -> parse("""
                {"scooterCode":1,"timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """)).isInstanceOf(JacksonException.class);
    }
}
