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

    /**
     * The operator retires the scooter: only an inactive scooter is retired, and a retired scooter
     * never changes state again, since no change starts from {@code RETIRED} (HE-37).
     */
    public Scooter retire() {
        return changeState(ScooterState.RETIRED, EnumSet.of(ScooterState.INACTIVE));
    }

    private Scooter changeState(ScooterState target, Set<ScooterState> allowedFrom) {
        if (!allowedFrom.contains(state)) {
            throw new ScooterStateChangeRefusedException(code, state, target);
        }
        return new Scooter(code, type, target, storeId);
    }
}
