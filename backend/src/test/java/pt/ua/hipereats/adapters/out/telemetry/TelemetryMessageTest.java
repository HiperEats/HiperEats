package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;
import tools.jackson.databind.json.JsonMapper;

/** Contract tests: the JSON of AGENTS.md section 6, as the simulator sends it, becomes a domain reading. */
class TelemetryMessageTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private TelemetryMessage parse(String payload) {
        return json.readValue(payload, TelemetryMessage.class);
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
}
