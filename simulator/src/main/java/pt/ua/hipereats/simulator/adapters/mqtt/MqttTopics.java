package pt.ua.hipereats.simulator.adapters.mqtt;

/** MQTT topic names of the telemetry contract (one set of topics per scooter). */
public final class MqttTopics {

    private static final String PREFIX = "hipereats/scooters/";

    /** Every scooter's commands; "+" is the MQTT single-level wildcard. */
    public static final String ALL_COMMANDS = PREFIX + "+/commands";

    private MqttTopics() {
    }

    public static String telemetry(String scooterCode) {
        return PREFIX + scooterCode + "/telemetry";
    }

    public static String commands(String scooterCode) {
        return PREFIX + scooterCode + "/commands";
    }

    public static String acks(String scooterCode) {
        return PREFIX + scooterCode + "/acks";
    }

    /** The scooter code in a topic such as {@code hipereats/scooters/AV-01/commands}. */
    public static String scooterCodeOf(String topic) {
        return topic.split("/")[2];
    }
}
