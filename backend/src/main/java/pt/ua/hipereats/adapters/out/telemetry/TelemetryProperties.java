package pt.ua.hipereats.adapters.out.telemetry;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection to the MQTT broker that carries the telemetry. In Docker Compose the broker URL comes
 * from the environment variable {@code HIPEREATS_TELEMETRY_BROKERURL}.
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
}
