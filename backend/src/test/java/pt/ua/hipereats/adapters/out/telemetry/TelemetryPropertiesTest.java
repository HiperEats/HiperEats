package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A wrong broker URL must stop the backend at startup, not leave it running without telemetry. */
class TelemetryPropertiesTest {

    private static TelemetryProperties withBrokerUrl(String brokerUrl) {
        return new TelemetryProperties(true, brokerUrl, "hipereats-backend", Duration.ofSeconds(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tcp://mosquitto:1883", "tcp://localhost:1883", "ssl://broker.example:8883",
            "ws://broker.example:9001", "wss://broker.example:443",
            "tcp://[::1]:1883"})
    void whenBrokerUrlIsValid_thenPropertiesAreAccepted(String brokerUrl) {
        assertThatNoException().isThrownBy(() -> withBrokerUrl(brokerUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mosquitto:1883", "mqtt://mosquitto:1883", "tcp://", "tcp://:1883", "tcp:mosquitto:1883", "",
            " ", "not a url"})
    void whenBrokerUrlIsInvalid_thenPropertiesAreRefused(String brokerUrl) {
        assertThatIllegalArgumentException().isThrownBy(() -> withBrokerUrl(brokerUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    void whenReconnectIntervalIsNotPositive_thenPropertiesAreRefused(String interval) {
        assertThatIllegalArgumentException().isThrownBy(() -> new TelemetryProperties(true,
                "tcp://localhost:1883", "hipereats-backend", Duration.parse("PT" + interval.toUpperCase())));
    }

    @Test
    void whenBrokerHostHasAnUnderscore_thenTheErrorSaysItIsNotSupported() {
        // Paho accepts this URL and then fails on every connection (Java 21).
        assertThatIllegalArgumentException().isThrownBy(() -> withBrokerUrl("tcp://my_broker:1883"))
                .withMessageContaining("underscore");
    }

    @ParameterizedTest
    @CsvSource({"PT5S, 4000", "PT4S, 4000", "PT3.999S, 2000", "PT1S, 1000",
            "PT0.2S, 1000", "PT2M, 64000", "PT24H, 128000"})
    void whenReconnectIntervalIsGiven_thenPahoNeverWaitsLongerThanIt(Duration interval, int expectedMillis) {
        // Paho starts at 1 s and doubles while it is below the maximum, so only 1, 2, 4, ... s are exact.
        TelemetryProperties properties = new TelemetryProperties(true, "tcp://localhost:1883", "test", interval);

        assertThat(properties.maxReconnectDelayMillis()).isEqualTo(expectedMillis);
    }
}
