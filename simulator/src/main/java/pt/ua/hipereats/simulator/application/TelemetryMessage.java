package pt.ua.hipereats.simulator.application;

/**
 * One telemetry reading, published on {@code hipereats/scooters/{scooterCode}/telemetry}.
 * Field names are the telemetry contract in AGENTS.md: do not rename them.
 *
 * @param timestamp ISO-8601 with offset, for example {@code 2026-11-25T14:00:05.123Z}
 * @param fault     fault code, or null when there is none
 */
public record TelemetryMessage(
        String scooterCode,
        String timestamp,
        int batteryPercent,
        double latitude,
        double longitude,
        boolean locked,
        String fault) {
}
