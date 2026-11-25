package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A wrong broker URL must stop the backend at startup, not leave it running without telemetry. */
class TelemetryPropertiesTest {

    private static TelemetryProperties withBrokerUrl(String brokerUrl) {
        return new TelemetryProperties(true, brokerUrl, "hipereats-backend", Duration.ofSeconds(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tcp://mosquitto:1883", "tcp://localhost:1883", "ssl://broker.example:8883",
            "ws://broker.example:9001", "wss://broker.example:443"})
    void whenBrokerUrlIsValid_thenPropertiesAreAccepted(String brokerUrl) {
        assertThatNoException().isThrownBy(() -> withBrokerUrl(brokerUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mosquitto:1883", "mqtt://mosquitto:1883", "tcp://", "", " ", "not a url"})
    void whenBrokerUrlIsInvalid_thenPropertiesAreRefused(String brokerUrl) {
        assertThatIllegalArgumentException().isThrownBy(() -> withBrokerUrl(brokerUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    void whenReconnectIntervalIsNotPositive_thenPropertiesAreRefused(String interval) {
        assertThatIllegalArgumentException().isThrownBy(() -> new TelemetryProperties(true,
                "tcp://localhost:1883", "hipereats-backend", Duration.parse("PT" + interval.toUpperCase())));
    }
}
