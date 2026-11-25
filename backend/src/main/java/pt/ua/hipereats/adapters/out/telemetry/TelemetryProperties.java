package pt.ua.hipereats.adapters.out.telemetry;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection to the MQTT broker that carries the telemetry. In Docker Compose the broker URL comes
 * from the environment variable {@code HIPEREATS_TELEMETRY_BROKERURL}.
 *
 * <p>Invalid values stop the backend at startup: a wrong broker URL would otherwise leave it
 * running without ever receiving telemetry.
 *
 * @param enabled           false turns the subscriber off
 * @param brokerUrl         for example {@code tcp://localhost:1883}
 * @param clientId          prefix of the MQTT client id (a random suffix is added)
 * @param reconnectInterval wait between connection attempts while the broker is unreachable
 */
@ConfigurationProperties("hipereats.telemetry")
public record TelemetryProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("tcp://localhost:1883") String brokerUrl,
        @DefaultValue("hipereats-backend") String clientId,
        @DefaultValue("5s") Duration reconnectInterval) {

    /** The URL schemes the MQTT client (Paho) supports. */
    private static final Set<String> SCHEMES = Set.of("tcp", "ssl", "ws", "wss");

    private static final int PAHO_FIRST_DELAY_MILLIS = 1_000;
    private static final int PAHO_LONGEST_DELAY_MILLIS = 128_000;

    public TelemetryProperties {
        if (!isValidBrokerUrl(brokerUrl)) {
            throw new IllegalArgumentException("hipereats.telemetry.broker-url must look like tcp://host:1883 "
                    + "(the MQTT client does not support an underscore in the host), not '" + brokerUrl + "'");
        }
        if (reconnectInterval == null || reconnectInterval.isNegative() || reconnectInterval.isZero()) {
            throw new IllegalArgumentException("hipereats.telemetry.reconnect-interval must be positive");
        }
    }

    /**
     * The maximum reconnect delay to give Paho, so that it never waits longer than {@code reconnectInterval}
     * (1 s at least, its first wait). Paho doubles the wait while it is below the maximum, so a maximum of
     * 5 s would wait 8 s: the largest of 1, 2, 4, ... 128 s that fits is used.
     */
    int maxReconnectDelayMillis() {
        int delay = PAHO_FIRST_DELAY_MILLIS;
        while (delay < PAHO_LONGEST_DELAY_MILLIS && delay * 2L <= reconnectInterval.toMillis()) {
            delay *= 2;
        }
        return delay;
    }

    private static boolean isValidBrokerUrl(String brokerUrl) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(brokerUrl);
            return uri.getScheme() != null && SCHEMES.contains(uri.getScheme()) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
