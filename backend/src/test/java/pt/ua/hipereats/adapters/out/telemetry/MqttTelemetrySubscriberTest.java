package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/** How one MQTT message is handled, without a broker. */
class MqttTelemetrySubscriberTest {

    /** Fake of the inbound port: remembers what it received. */
    private static class RecordingUseCase implements RecordTelemetryUseCase {
        final List<TelemetryReading> received = new ArrayList<>();

        @Override
        public Result record(TelemetryReading reading) {
            received.add(reading);
            return Result.RECORDED;
        }
    }

    private final RecordingUseCase useCase = new RecordingUseCase();
    private final MqttTelemetrySubscriber subscriber = new MqttTelemetrySubscriber(
            new TelemetryProperties(true, "tcp://localhost:1883", "test", Duration.ofSeconds(5)), useCase);

    private static byte[] telemetryOf(String scooterCode) {
        return ("""
                {"scooterCode":"%s","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """.formatted(scooterCode)).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void whenMessageArrivesOnItsScooterTopic_thenItIsRecorded() {
        subscriber.handle("hipereats/scooters/AV-01/telemetry", telemetryOf("AV-01"));

        assertThat(useCase.received).extracting(TelemetryReading::scooterCode).containsExactly("AV-01");
    }

    @Test
    void whenMessageIsAboutAnotherScooterThanItsTopic_thenItIsDropped() {
        subscriber.handle("hipereats/scooters/AV-01/telemetry", telemetryOf("AV-02"));

        assertThat(useCase.received).isEmpty();
    }

    @Test
    void whenMessageIsInvalid_thenItIsDroppedWithoutError() {
        assertThatNoException().isThrownBy(() -> subscriber.handle("hipereats/scooters/AV-01/telemetry",
                "this is not json".getBytes(StandardCharsets.UTF_8)));

        assertThat(useCase.received).isEmpty();
    }
}
