package pt.ua.hipereats.simulator;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Simulation parameters (Product Specification, 4.3). Defaults are in application.yml; every value
 * can be overridden by an environment variable, for example {@code SIMULATOR_TELEMETRYINTERVAL=1s}
 * or {@code SIMULATOR_MQTT_BROKERURL=tcp://mosquitto:1883}.
 */
@ConfigurationProperties("simulator")
public record SimulatorProperties(
        Mqtt mqtt,
        Duration telemetryInterval,
        double speedKmh,
        long seed,
        double faultProbability,
        double standardBatteryUsePerKm,
        double longRangeBatteryUsePerKm,
        double initialBatteryMin,
        double initialBatteryMax,
        Delivery delivery,
        List<StoreProperties> stores) {

    /** Connection to the MQTT broker. */
    public record Mqtt(String brokerUrl, String clientId) {
    }

    /** Share of deliveries per distance band: near (up to 2 km), middle (2 to 3 km); the rest are far (beyond 3 km). */
    public record Delivery(double nearShare, double middleShare) {
    }

    /** A store and its fleet. */
    public record StoreProperties(String code, String name, double latitude, double longitude,
                                  int standardScooters, int longRangeScooters) {
    }
}
