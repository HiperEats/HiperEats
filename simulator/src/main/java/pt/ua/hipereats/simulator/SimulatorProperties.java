package pt.ua.hipereats.simulator;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Simulation parameters (Product Specification, 4.3). Defaults are in application.yml; every value
 * can be overridden by an environment variable, for example {@code SIMULATOR_TELEMETRYINTERVAL=1s}
 * or {@code SIMULATOR_MQTT_BROKERURL=tcp://mosquitto:1883}.
 *
 * <p>Values are checked here, so a wrong configuration stops the simulator at startup. The list of
 * stores is replaced as a whole: to change one store by environment variables, give every field of
 * every store.
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

    public SimulatorProperties {
        require(mqtt != null, "simulator.mqtt is required");
        require(delivery != null, "simulator.delivery is required");
        require(telemetryInterval != null && telemetryInterval.isPositive(),
                "simulator.telemetry-interval must be positive");
        require(speedKmh > 0, "simulator.speed-kmh must be positive");
        require(faultProbability >= 0 && faultProbability <= 1, "simulator.fault-probability must be 0 to 1");
        require(standardBatteryUsePerKm > 0 && longRangeBatteryUsePerKm > 0,
                "battery use per km must be positive");
        require(initialBatteryMin >= 0 && initialBatteryMin <= initialBatteryMax && initialBatteryMax <= 100,
                "initial battery must satisfy 0 <= min <= max <= 100");
        require(stores != null && !stores.isEmpty(), "simulator.stores needs at least one store");
        Set<String> codes = new HashSet<>();
        for (StoreProperties store : stores) {
            require(store != null && codes.add(store.code()), "store codes must be unique");
        }
    }

    /**
     * Connection to the MQTT broker.
     *
     * @param reconnectInterval longest wait between reconnection attempts after the connection drops
     */
    public record Mqtt(String brokerUrl, String clientId, Duration reconnectInterval) {
        public Mqtt {
            require(brokerUrl != null && !brokerUrl.isBlank(), "simulator.mqtt.broker-url is required");
            require(clientId != null && !clientId.isBlank(), "simulator.mqtt.client-id is required");
            require(reconnectInterval != null && reconnectInterval.isPositive(),
                    "simulator.mqtt.reconnect-interval must be positive");
        }
    }

    /**
     * Delivery distances.
     *
     * @param nearShare      share of orders up to {@code standardMaxKm}
     * @param middleShare    share of orders between {@code standardMaxKm} and {@code longRangeMaxKm};
     *                       the rest are beyond the delivery radius
     * @param standardMaxKm  farthest delivery of a standard scooter (BR14)
     * @param longRangeMaxKm delivery radius, farthest delivery of a long-range scooter (BR2)
     * @param rangeMargin    margin on the round trip (BR1: 1.2 means 20 %)
     */
    public record Delivery(double nearShare, double middleShare, double standardMaxKm, double longRangeMaxKm,
                           double rangeMargin) {
        public Delivery {
            require(nearShare >= 0 && middleShare >= 0 && nearShare + middleShare <= 1,
                    "delivery shares must be positive and add up to 1 at most");
            require(standardMaxKm > 0 && standardMaxKm <= longRangeMaxKm,
                    "simulator.delivery: 0 < standard-max-km <= long-range-max-km");
            require(rangeMargin >= 1, "simulator.delivery.range-margin must be at least 1");
        }
    }

    /** A store and its fleet. */
    public record StoreProperties(String code, String name, double latitude, double longitude,
                                  int standardScooters, int longRangeScooters) {
        public StoreProperties {
            require(code != null && !code.isBlank(), "every store needs a code (give every field of every store)");
            require(name != null && !name.isBlank(), "store " + code + " needs a name");
            require(latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180,
                    "store " + code + " has invalid coordinates");
            require(standardScooters >= 0 && longRangeScooters >= 0,
                    "store " + code + " cannot have a negative number of scooters");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
