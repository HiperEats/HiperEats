package pt.ua.hipereats.domain.fleet;

import java.util.EnumSet;
import java.util.Set;

/**
 * A scooter of the fleet.
 *
 * <p>Each change of state names the states it may start from (Product Specification, 3.3); any
 * other change is refused.
 *
 * @param code    identifies the scooter on the platform and in its telemetry ({@code scooterCode})
 * @param storeId the store whose fixed fleet the scooter belongs to
 */
public record Scooter(String code, ScooterType type, ScooterState state, long storeId) {

    /** The operator activates the scooter: only an inactive scooter becomes available (HE-30). */
    public Scooter activate() {
        return changeState(ScooterState.AVAILABLE, EnumSet.of(ScooterState.INACTIVE));
    }

    /**
     * The operator deactivates the scooter: only an available scooter or one in maintenance becomes
     * inactive, so a scooter in use, or reserved for a courier, is refused (HE-30).
     */
    public Scooter deactivate() {
        return changeState(ScooterState.INACTIVE,
                EnumSet.of(ScooterState.AVAILABLE, ScooterState.MAINTENANCE));
    }

    private Scooter changeState(ScooterState target, Set<ScooterState> allowedFrom) {
        if (!allowedFrom.contains(state)) {
            throw new ScooterStateChangeRefusedException(code, state, target);
        }
        return new Scooter(code, type, target, storeId);
    }
}
