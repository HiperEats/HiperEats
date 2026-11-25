package pt.ua.hipereats.domain.telemetry;

import java.time.OffsetDateTime;

/**
 * One telemetry reading reported by a scooter: battery, position, lock state and fault.
 * A reading that cannot be real (battery above 100 %, latitude above 90°, NaN, ...) is refused.
 *
 * @param fault fault code reported by the scooter, or null when there is none (never blank)
 */
public record TelemetryReading(
        String scooterCode,
        OffsetDateTime timestamp,
        int batteryPercent,
        double latitude,
        double longitude,
        boolean locked,
        String fault) {

    public TelemetryReading {
        if (scooterCode == null || scooterCode.isBlank()) {
            throw new IllegalArgumentException("scooterCode is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (batteryPercent < 0 || batteryPercent > 100) {
            throw new IllegalArgumentException("batteryPercent must be between 0 and 100: " + batteryPercent);
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180: " + longitude);
        }
        if (fault != null && fault.isBlank()) {
            throw new IllegalArgumentException("fault must be null or a fault code, not blank");
        }
    }

    /** True if this reading was taken after {@code other}; a reading with the same time is not newer. */
    public boolean isNewerThan(TelemetryReading other) {
        return timestamp.isAfter(other.timestamp);
    }

    public boolean hasFault() {
        return fault != null;
    }
}
