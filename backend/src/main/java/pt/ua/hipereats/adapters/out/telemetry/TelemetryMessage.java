package pt.ua.hipereats.adapters.out.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON message of the telemetry contract (AGENTS.md, section 6), as it arrives on
 * {@code hipereats/scooters/{scooterCode}/telemetry}. Boxed types, so a missing field is detected.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryMessage(
        String scooterCode,
        String timestamp,
        Integer batteryPercent,
        Double latitude,
        Double longitude,
        Boolean locked,
        String fault) {

    /**
     * Strict JSON reader: no conversion between types, so {@code "76"}, {@code 100.9} as a battery or
     * {@code 1} as {@code locked} are refused instead of being turned into valid-looking values.
     */
    private static final JsonMapper STRICT_JSON = JsonMapper.builder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    /**
     * Reads one MQTT payload.
     *
     * @throws tools.jackson.core.JacksonException if it is not valid JSON or a field has the wrong type
     */
    public static TelemetryMessage parse(byte[] payload) {
        return STRICT_JSON.readValue(payload, TelemetryMessage.class);
    }

    /**
     * Converts the message into a domain reading.
     *
     * @throws IllegalArgumentException if a field is missing or invalid
     * @throws java.time.format.DateTimeParseException if the timestamp is not ISO-8601 with offset
     */
    public TelemetryReading toReading() {
        if (timestamp == null || batteryPercent == null || latitude == null || longitude == null || locked == null) {
            throw new IllegalArgumentException("Telemetry message with a missing field: " + this);
        }
        return new TelemetryReading(scooterCode, OffsetDateTime.parse(timestamp), batteryPercent,
                latitude, longitude, locked, fault);
    }
}
