package pt.ua.hipereats.adapters.out.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

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
