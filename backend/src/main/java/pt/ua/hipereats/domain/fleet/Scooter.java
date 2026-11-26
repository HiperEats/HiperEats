package pt.ua.hipereats.domain.fleet;

/**
 * A scooter of the fleet.
 *
 * @param code    identifies the scooter on the platform and in its telemetry ({@code scooterCode})
 * @param storeId the store whose fixed fleet the scooter belongs to
 */
public record Scooter(String code, ScooterType type, ScooterState state, long storeId) {
}
